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

import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.AutoPopulated;
import com.adaptris.annotation.InputFieldDefault;
import com.adaptris.annotation.InputFieldHint;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisPollingConsumer;
import com.adaptris.core.CoreException;
import com.adaptris.core.util.Args;
import com.adaptris.interlok.resolver.ExternalResolver;
import com.adaptris.mail.JavamailReceiverFactory;
import com.adaptris.mail.MailException;
import com.adaptris.mail.MailReceiver;
import com.adaptris.mail.MailReceiverFactory;
import com.adaptris.mail.MatchProxyFactory;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import jakarta.mail.internet.MimeMessage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Closeable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.split;

/**
 * Email implementation of the AdaptrisMessageConsumer interface.
 * <p>
 * The consume destination configuration is the mailbox url in the form {@code  pop3|imap://user:password@host:port/mailbox} where
 * mailbox follows the rule {@code url.getProtocol() == "pop3" ? "INBOX" : "any arbitary folder"}
 * </p>
 * <p>
 * Possible filter expressions could be :-
 * <ul>
 * <li>&lt;filter-expression&gt;*&lt;filter-expression&gt;</li>
 * <li>&lt;filter-expression&gt;FROM=somevalue,RECIPIENT=somevalue,SUBJECT=somevalue &lt;filter-expression&gt;</li>
 * <li>&lt;filter-expression&gt;FROM=somevalue,SUBJECT=somevalue &lt;filter-expression&gt;</li>
 * <li>&lt;filter-expression&gt;FROM=somevalue&lt;filter-expression&gt;</li>
 * <li>&lt;filter-expression&gt;SUBJECT=somevalue&lt;filter-expression&gt;</li>
 * <li>&lt;filter-expression&gt;RECIPIENT=somevalue&lt;filter-expression&gt;</li>
 * </ul>
 * A missing filter-expression implicitly means all. If you are performing filtering on the recipient, then an attempt will be made
 * to match the filter expression against all the recipients for the email.
 * </p>
 * <p>
 * The default filter-expression syntax is based on {@code java.util.Pattern}. It can be changed by using the
 * {@link #setRegularExpressionStyle(String)} method.
 * <p>
 * It is possible to control the underlying behaviour of this consumer through the use of various properties that will be passed to
 * the <code>jakarta.mail.Session</code> instance. You need to refer to the javamail documentation to see a list of the available
 * properties and meanings.
 * </p>
 */

public abstract class MailConsumerImp extends AdaptrisPollingConsumer{

  private static final String RECIPIENT = "RECIPIENT";
  private static final String SUBJECT = "SUBJECT";
  private static final String FROM = "FROM";


  // marshalled
  @AdvancedConfig
  @InputFieldDefault(value = "false")
  private Boolean deleteOnReceive; // false
  @AdvancedConfig
  @InputFieldDefault(value = "true")
  private Boolean attemptConnectOnInit;
  @AdvancedConfig
  private String regularExpressionStyle;
  @InputFieldHint(style = "PASSWORD", external = true)
  private String password;
  private String username;
  @NotNull
  @AutoPopulated
  @Valid
  private MailReceiverFactory mailReceiverFactory;
  @AdvancedConfig
  @Valid
  @InputFieldDefault(value = "ignore-mail-headers")
  private MailHeaderHandler headerHandler;

  /**
   * The Mailbox specified as a URL.
   *
   */
  @Getter
  @Setter
  @NotBlank
  private String mailboxUrl;

  /**
   * The filter expression to use when listing files.
   * <p>
   * If not specified then will default in a file filter that matches all files.
   * </p>
   */
  @Getter
  @Setter
  private String filterExpression;


  protected transient MailReceiver mbox;

  public MailConsumerImp() {
    setRegularExpressionStyle(MatchProxyFactory.DEFAULT_REGEXP_STYLE);
    regularExpressionStyle = MatchProxyFactory.DEFAULT_REGEXP_STYLE;
    mailReceiverFactory = new JavamailReceiverFactory();
    setHeaderHandler(new IgnoreMailHeaders());
  }

  @Override
  protected void prepareConsumer() throws CoreException {
  }

  @Override
  protected int processMessages() {
    int count = 0;

    try (Closeable c = mbox) {
      mbox.connect();
      // log.trace("there are {} messages to process", mbox.getMessages().size());
      for (MimeMessage msg : mbox) {
        try {
          mbox.setMessageRead(msg);
          List<AdaptrisMessage> msgs = createMessages(msg);
          for (AdaptrisMessage m : msgs) {
            retrieveAdaptrisMessageListener().onAdaptrisMessage(m);
          }
        }
        catch (MailException e) {
          mbox.resetMessage(msg);
          log.debug("Error processing {}",(msg != null ? msg.getMessageID() : "<null>"), e);
        }
        if (!continueProcessingMessages(++count)) {
          break;
        }
      }
    }
    catch (Exception e) {
      log.warn("Error reading mailbox; will retry on next poll", e);
    }
    return count;
  }

  protected abstract List<AdaptrisMessage> createMessages(MimeMessage mime)
      throws MailException, CoreException;

  protected abstract void initConsumer() throws CoreException;

  /**
   * @see com.adaptris.core.AdaptrisComponent#init()
   */
  @Override
  public final void init() throws CoreException {

    try {
      mbox = getMailReceiverFactory().createClient(
          MailHelper.createURLName(mailboxUrl(), getUsername(),
              ExternalResolver.resolve(getPassword())));
      //mbox = new MailComNetClient("localhost", 3110, getUsername(), Password.decode(getPassword()));
      mbox.setRegularExpressionCompiler(regularExpressionStyle());
      Map<String, String> filters = initFilters(filterExpression());

      log.trace("From filter set to [{}]", filters.get(FROM));
      log.trace("Subject filter set to [{}]", filters.get(SUBJECT));
      log.trace("Recipient filter set to [{}]", filters.get(RECIPIENT));

      mbox.setFromFilter(filters.get(FROM));
      mbox.setSubjectFilter(filters.get(SUBJECT));
      mbox.setRecipientFilter(filters.get(RECIPIENT));
      // Make an attempt to connect just to be sure that we can
      if (attemptConnectOnInit()) {
        try (Closeable c = mbox) {
          mbox.connect();
        }
      }
      mbox.purge(deleteOnReceive());
    }
    catch (Exception e) {
      throw new CoreException(e);
    }
    initConsumer();
    super.init();
  }


  /**
   * Set the filtering on the consume destination.
   */
  private Map<String, String> initFilters(String filterString) {
    Map<String, String> result = new HashMap<String, String>();
    if (isBlank(filterString) || filterString.equalsIgnoreCase("*")) {
      return result;
    }
    String[] filters = split(filterString, ",");
    for (String f : filters) {
      addFilter(result, split(f, "=", 2));
    }
    return result;
  }

  private void addFilter(Map<String, String> filters, String[] nv) {
    if (nv.length < 2) {
      return;
    }
    filters.put(defaultIfEmpty(nv[0], ""), defaultIfEmpty(nv[1], ""));
  }

  /**
   * Specify whether messages should be deleted after receipt.
   *
   * @param b true or false.
   */
  public void setDeleteOnReceive(Boolean b) {
    deleteOnReceive = b;
  }

  /**
   * Get the flag.
   *
   * @return true or false.
   */
  public Boolean getDeleteOnReceive() {
    return deleteOnReceive;
  }

  boolean deleteOnReceive() {
    return BooleanUtils.toBooleanDefaultIfNull(getDeleteOnReceive(), false);
  }

  /**
   * returns the regularExpressionSyntax.
   *
   * @return returns the regular expression style
   * @see #setRegularExpressionStyle(String)
   */
  public String getRegularExpressionStyle() {
    return regularExpressionStyle;
  }

  /**
   * Set the regular expression syntax.
   *
   * @param s The regular expression style to set, defaults to "Regex" ({@code java.util.regex.Pattern}) if not specified.
   */
  public void setRegularExpressionStyle(String s) {
    regularExpressionStyle = s;
  }

  String regularExpressionStyle() {
    return StringUtils.defaultIfBlank(getRegularExpressionStyle(), MatchProxyFactory.DEFAULT_REGEXP_STYLE);
  }

  public String getPassword() {
    return password;
  }

  /**
   * Set the password to be used with this consumer implementation.
   * <p>
   * If you specify the username and password in the URL for the SMTP server then does not lend itself to being encrypted. Specify
   * the password here if you wish to use {@link com.adaptris.security.password.Password#decode(String)} to decode the password.
   * </p>
   *
   * @param pw the password.
   */
  public void setPassword(String pw) {
    password = pw;
  }

  public String getUsername() {
    return username;
  }

  /**
   * Set the username to be used with this consumer implementation.
   *
   * @param name the username.
   */
  public void setUsername(String name) {
    username = name;
  }

  public Boolean getAttemptConnectOnInit() {
    return attemptConnectOnInit;
  }

  /**
   * Specify whether or not to attempt a connection upon {@link #init()}.
   *
   * @param b true to attempt a connection on init, false otherwise, default is true if unspecified.
   */
  public void setAttemptConnectOnInit(Boolean b) {
    attemptConnectOnInit = b;
  }

  boolean attemptConnectOnInit() {
    return BooleanUtils.toBooleanDefaultIfNull(getAttemptConnectOnInit(), true);
  }

  public MailReceiverFactory getMailReceiverFactory() {
    return mailReceiverFactory;
  }

  /**
   * Set the type of client to use to connect to the mailbox.
   *
   * @param f the {@link MailReceiverFactory}, default is a {@link JavamailReceiverFactory}
   */
  public void setMailReceiverFactory(MailReceiverFactory f) {
    mailReceiverFactory = f;
  }

  /**
   *
   * @since 3.6.5
   */
  public MailHeaderHandler getHeaderHandler() {
    return headerHandler;
  }

  /**
   * Specify how to handle mails headers
   *
   * @param mh the handler, defaults to {@link IgnoreMailHeaders}.
   * @since 3.6.5
   */
  public void setHeaderHandler(MailHeaderHandler mh) {
    headerHandler = Args.notNull(mh, "headerHandler");
  }

  protected MailHeaderHandler headerHandler() {
    return getHeaderHandler();
  }


  @Override
  protected String newThreadName() {
    return retrieveAdaptrisMessageListener().friendlyName();
  }

  protected String mailboxUrl() {
    return getMailboxUrl();
  }

  protected String filterExpression() {
    return getFilterExpression();
  }

}
