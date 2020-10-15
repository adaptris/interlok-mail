package com.adaptris.core.mail;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.annotation.InputFieldDefault;
import com.adaptris.annotation.InputFieldHint;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.CoreException;
import com.adaptris.core.MultiPayloadAdaptrisMessage;
import com.adaptris.core.ProduceException;
import com.adaptris.core.ProduceOnlyProducerImp;
import com.microsoft.aad.msal4j.IAccount;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.MsalException;
import com.microsoft.aad.msal4j.PublicClientApplication;
import com.microsoft.aad.msal4j.SilentParameters;
import com.microsoft.aad.msal4j.UserNamePasswordParameters;
import com.microsoft.graph.models.extensions.Attachment;
import com.microsoft.graph.models.extensions.EmailAddress;
import com.microsoft.graph.models.extensions.FileAttachment;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.models.extensions.ItemBody;
import com.microsoft.graph.models.extensions.Message;
import com.microsoft.graph.models.extensions.Recipient;
import com.microsoft.graph.models.generated.BodyType;
import com.microsoft.graph.requests.extensions.GraphServiceClient;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotBlank;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Implementation of an email producer that is geared towards Microsoft
 * Office 365, using their Graph API and OAuth2.
 *
 * @config office-365-mail-producer
 */
@XStreamAlias("office-365-mail-producer")
@AdapterComponent
@ComponentProfile(summary = "Send email using a Microsoft Office 365 account using the Microsoft Graph API", tag = "producer,email,o365,microsoft,office,outlook,365")
@DisplayOrder(order = { "clientId", "tenantId", "clientSecret", "scope", "username", "password" })
public class O365MailProducer extends ProduceOnlyProducerImp
{
  private static final String DEFAULT_TENANT = "common";
  private static final String[] DEFAULT_SCOPES = { "Mail.Send" };

  @Getter
  @Setter
  @NotBlank
  private String clientId;

  @Getter
  @Setter
  @NotBlank
  private String tenantId;

  @Getter
  @Setter
  @NotBlank
  private String clientSecret;

  @Getter
  @Setter
  @AdvancedConfig(rare = true)
//  @InputFieldDefault(Arrays.stream(DEFAULT_SCOPES).reduce((a, b) -> a + "," + b))
  private Set<String> scopes;

  @Getter
  @Setter
  @NotBlank
  private String username;

  @Getter
  @Setter
  @NotBlank
  private String password;

  @Getter
  @Setter
  @NotBlank
  // FIXME remove before release!
  private String accessToken;

  @Getter
  @Setter
  @InputFieldHint(expression = true)
  private String subject;

  @Getter
  @Setter
  @InputFieldHint(expression = true)
  private String toRecipients;

  @Getter
  @Setter
  @AdvancedConfig
  @InputFieldHint(expression = true)
  private String ccRecipients;

  @Getter
  @Setter
  @AdvancedConfig
  @InputFieldHint(expression = true)
  private String bccRecipients;

  @Getter
  @Setter
  @AdvancedConfig
  @InputFieldHint(friendly = "Save message to sent items?")
  @InputFieldDefault("true")
  private Boolean save;

  private transient PublicClientApplication application;
  private transient IAccount account;

  @Override
  public void prepare() throws CoreException
  {
    try
    {
      application = PublicClientApplication.builder(clientId).authority(tenant()).build();
      Set<IAccount> accountsInCache = application.getAccounts().join();
      account = getAccountByUsername(accountsInCache);
    }
    catch (Exception e)
    {
      log.error("Could not identify Azure application or tenant", e);
      throw new CoreException(e);
    }
  }

  @Override
  protected void doProduce(AdaptrisMessage adaptrisMessage, String endpoint) throws ProduceException
  {
    try
    {

      /*
       * FIXME Figure out how to actually get a valid token!  Either as
       *  an application that can access all mailboxes for all AD users
       *  or as a user that can access their own mailbox, but without a
       *  prompt being displayed.
       */
//      IAuthenticationResult iAuthResult = getAccessToken(application, account);
//
//      log.info("Access token:     " + iAuthResult.accessToken());
//      log.info("Id token:         " + iAuthResult.idToken());
//      log.info("Account username: " + iAuthResult.account().username());

      IGraphServiceClient graphClient = GraphServiceClient.builder().authenticationProvider(request -> request.addHeader("Authorization", "Bearer " + accessToken)).buildClient();

      Message outlookMessage = new Message();
      outlookMessage.subject = adaptrisMessage.resolve(subject);

      ItemBody body = new ItemBody();
      body.contentType = BodyType.TEXT;
      body.content = adaptrisMessage.getContent();
      outlookMessage.body = body;

      outlookMessage.toRecipients = resolveRecipients(adaptrisMessage, toRecipients);
      outlookMessage.ccRecipients = resolveRecipients(adaptrisMessage, ccRecipients);
      outlookMessage.bccRecipients = resolveRecipients(adaptrisMessage, bccRecipients);

      // TODO Add any custom headers.

      if (adaptrisMessage instanceof MultiPayloadAdaptrisMessage)
      {
        MultiPayloadAdaptrisMessage multiPayloadAdaptrisMessage = (MultiPayloadAdaptrisMessage)adaptrisMessage;
        String id = multiPayloadAdaptrisMessage.getCurrentPayloadId();
        List<Attachment> attachments = new LinkedList<>();
        for (String name : multiPayloadAdaptrisMessage.getPayloadIDs())
        {
          if (name.equals(id))
          {
            continue;
          }
          FileAttachment attachment = new FileAttachment();
          attachment.name = name;
          attachment.contentType = "application/octet-stream";//"text/plain";
          attachment.contentBytes = multiPayloadAdaptrisMessage.getPayload(name);
          attachments.add(attachment);
          outlookMessage.attachments.getCurrentPage().add(attachment);
        }
      }

      graphClient.users(username).sendMail(outlookMessage, save()).buildRequest();//.post();
    }
    catch (Exception e)
    {
      log.error("Exception processing Outlook message", e);
      throw new ProduceException(e);
    }
  }

  @Override
  public String endpoint(AdaptrisMessage adaptrisMessage)
  {
    return adaptrisMessage.resolve(toRecipients);
  }

  private static List<Recipient> resolveRecipients(AdaptrisMessage adaptrisMessage, String recipients)
  {
    if (StringUtils.isEmpty(recipients))
    {
      return null;
    }
    List<Recipient> recipientList = new LinkedList<>();
    for (String address : adaptrisMessage.resolve(recipients).split(","))
    {
      Recipient recipient = new Recipient();
      EmailAddress emailAddress = new EmailAddress();
      emailAddress.address = address;
      recipient.emailAddress = emailAddress;
      recipientList.add(recipient);
    }
    return recipientList;
  }

  /**
   * Helper function to return an account from a given set of accounts based on the given username,
   * or return null if no accounts in the set match
   */
  private IAccount getAccountByUsername(Set<IAccount> accounts)
  {
    if (accounts.isEmpty())
    {
      log.debug("No accounts in cache");
    }
    else
    {
      log.debug("Total accounts in cache: " + accounts.size());
      for (IAccount account : accounts)
      {
        if (account.username().equals(username))
        {
          return account;
        }
      }
    }
    return null;
  }

  private IAuthenticationResult getAccessToken(PublicClientApplication pca, IAccount account) throws Exception
  {
    IAuthenticationResult result;
    try
    {
      SilentParameters silentParameters = SilentParameters.builder(scopes()).account(account).build();
      // Try to acquire token silently. This will fail on the first acquireTokenUsernamePassword() call
      // because the token cache does not have any data for the user you are trying to acquire a token for
      result = pca.acquireTokenSilently(silentParameters).join();
    }
    catch (Exception ex)
    {
      if (ex.getCause() instanceof MsalException)
      {
        UserNamePasswordParameters parameters = UserNamePasswordParameters.builder(scopes(), username, password.toCharArray()).build();
        // Try to acquire a token via username/password.
        result = pca.acquireToken(parameters).join();
      }
      else
      {
        // Handle other exceptions accordingly
        throw ex;
      }
    }
    return result;
  }

  private String tenant()
  {
    return String.format("https://login.microsoftonline.com/%s", StringUtils.defaultString(tenantId, DEFAULT_TENANT));
  }

  private Set<String> scopes()
  {
    return scopes != null ? scopes : Set.of(DEFAULT_SCOPES);
  }

  private boolean save()
  {
    return BooleanUtils.toBooleanDefaultIfNull(save, true);
  }
}
