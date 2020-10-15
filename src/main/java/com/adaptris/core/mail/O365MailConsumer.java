/*
 * Copyright 2015 Adaptris Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package com.adaptris.core.mail;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.annotation.InputFieldDefault;
import com.adaptris.annotation.InputFieldHint;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisPollingConsumer;
import com.adaptris.core.CoreException;
import com.adaptris.core.MultiPayloadAdaptrisMessage;
import com.adaptris.core.util.DestinationHelper;
import com.microsoft.aad.msal4j.*;
import com.microsoft.graph.models.extensions.Attachment;
import com.microsoft.graph.models.extensions.FileAttachment;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.models.extensions.InternetMessageHeader;
import com.microsoft.graph.models.extensions.Message;
import com.microsoft.graph.requests.extensions.GraphServiceClient;
import com.microsoft.graph.requests.extensions.IAttachmentCollectionPage;
import com.microsoft.graph.requests.extensions.IAttachmentRequest;
import com.microsoft.graph.requests.extensions.IMessageCollectionPage;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Getter;
import lombok.Setter;
import lombok.With;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotBlank;
import java.net.URL;
import java.util.Base64;
import java.util.Set;

/**
 * Implementation of an email consumer that is geared towards Microsoft
 * Office 365, using their Graph API and OAuth2.
 *
 * @config office-365-mail-consumer
 */
@XStreamAlias("office-365-mail-consumer")
@AdapterComponent
@ComponentProfile(summary = "Pickup email from a Microsoft Office 365 account using the Microsoft Graph API", tag = "consumer,email,o365,microsoft,office,outlook,365")
@DisplayOrder(order = { "clientId", "tenantId", "clientSecret", "scope", "username", "password" })
public class O365MailConsumer extends AdaptrisPollingConsumer
{
  private static final String DEFAULT_TENANT = "common";
  private static final String[] DEFAULT_SCOPES = { "Mail.Read", "Mail.ReadBasic", "Mail.ReadWrite", "Mail.Read.All", "Mail.ReadBasic.All" };

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
  @AdvancedConfig
  @InputFieldHint(friendly = "Delete messages instead of marking them as read?")
  @InputFieldDefault("false")
  private Boolean delete;

  private transient PublicClientApplication application;
  private transient IAccount account;

  @Override
  protected void prepareConsumer() throws CoreException
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
  protected int processMessages()
  {
    int count = 0;
    try
    {
//      IAuthenticationResult iAuthResult = getAccessToken(application, account);
//
//      log.info("Access token:     " + iAuthResult.accessToken());
//      log.info("Id token:         " + iAuthResult.idToken());
//      log.info("Account username: " + iAuthResult.account().username());

      IGraphServiceClient graphClient = GraphServiceClient.builder().authenticationProvider(request -> request.addHeader("Authorization", "Bearer " + accessToken)).buildClient();

      IMessageCollectionPage messages = graphClient.users(username).mailFolders("inbox").messages().buildRequest().filter("isRead eq false").get();

      // TODO handle multiple pages...
      for (Message outlookMessage : messages.getCurrentPage())
      {
        log.debug("Found {} messages on page", messages.getCurrentPage().size());

        String id = outlookMessage.id;
        AdaptrisMessage adaptrisMessage = getMessageFactory().newMessage(outlookMessage.body.content);

        if (adaptrisMessage instanceof MultiPayloadAdaptrisMessage)
        {
          ((MultiPayloadAdaptrisMessage)adaptrisMessage).setCurrentPayloadId(id);
        }
        adaptrisMessage.addMetadata(EmailConstants.EMAIL_MESSAGE_ID, id);
        adaptrisMessage.addMetadata("Subject", outlookMessage.subject);
        adaptrisMessage.addMetadata("To", outlookMessage.toRecipients.stream().map(r -> r.emailAddress.address).reduce((a, b) -> a + "," + b).get() );
        adaptrisMessage.addMetadata("From", outlookMessage.from.emailAddress.address);
        adaptrisMessage.addMetadata("CC", String.join(",", outlookMessage.ccRecipients.stream().map(r -> r.emailAddress.address).toArray(String[]::new)));

        if (adaptrisMessage instanceof MultiPayloadAdaptrisMessage && outlookMessage.hasAttachments)
        {
          MultiPayloadAdaptrisMessage multiPayloadAdaptrisMessage = (MultiPayloadAdaptrisMessage)adaptrisMessage;
          IAttachmentCollectionPage attachments = graphClient.users(username).messages(id).attachments().buildRequest().get();
          for (Attachment attachment : attachments.getCurrentPage())
          {
            if (attachment instanceof FileAttachment)
            {
              byte[] bytes = Base64.getDecoder().decode(((FileAttachment)attachment).contentBytes);
              multiPayloadAdaptrisMessage.addPayload(attachment.name, bytes);
            }
          }
          multiPayloadAdaptrisMessage.switchPayload(id);
        }

        /*
         * The internetMessageHeaders need to be requested explicitly with a SELECT.
         */
        outlookMessage = graphClient.users(username).messages(id).buildRequest().select("internetMessageHeaders").get();
        if (outlookMessage.internetMessageHeaders != null)
        {
          for (InternetMessageHeader header : outlookMessage.internetMessageHeaders)
          {
            adaptrisMessage.addMetadata(header.name, header.value);
          }
        }

        retrieveAdaptrisMessageListener().onAdaptrisMessage(adaptrisMessage);

        if (delete())
        {
          graphClient.users(username).messages(id).buildRequest().delete();
        }
        else
        {
          /*
           * With PATCH, only send what we've changed, in this instance
           * we're marking the mail as read.
           */
          outlookMessage = new Message();
          outlookMessage.isRead = true;
          graphClient.users(username).messages(id).buildRequest().patch(outlookMessage);
        }

        count++;
      }

    }
    catch (Throwable e)
    {
      log.error("Exception processing Outlook message", e);
    }

    return count;
  }

  @Override
  protected String newThreadName()
  {
    return DestinationHelper.threadName(retrieveAdaptrisMessageListener(), null);
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

  private boolean delete()
  {
    return BooleanUtils.toBooleanDefaultIfNull(delete, false);
  }
}

