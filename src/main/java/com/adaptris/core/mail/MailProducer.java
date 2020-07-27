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

import java.util.Iterator;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;
import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.AutoPopulated;
import com.adaptris.annotation.InputFieldHint;
import com.adaptris.annotation.Removal;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.CoreConstants;
import com.adaptris.core.CoreException;
import com.adaptris.core.MetadataCollection;
import com.adaptris.core.MetadataElement;
import com.adaptris.core.ProduceDestination;
import com.adaptris.core.ProduceException;
import com.adaptris.core.ProduceOnlyProducerImp;
import com.adaptris.core.metadata.MetadataFilter;
import com.adaptris.core.metadata.RemoveAllMetadataFilter;
import com.adaptris.core.util.Args;
import com.adaptris.core.util.DestinationHelper;
import com.adaptris.core.util.ExceptionHelper;
import com.adaptris.core.util.LoggingHelper;
import com.adaptris.interlok.resolver.ExternalResolver;
import com.adaptris.mail.MailException;
import com.adaptris.mail.SmtpClient;
import com.adaptris.security.exc.PasswordException;
import com.adaptris.util.KeyValuePair;
import com.adaptris.util.KeyValuePairSet;
import lombok.Getter;
import lombok.Setter;

/**
 * Abstract implementation of the AdaptrisMessageProducer interface for handling Email.
 * <p>
 * Because email is implicitly asynchronous, Request-Reply is invalid, and as such if the request method is used, an
 * <code>UnsupportedOperationException</code> is thrown.
 * </p>
 * <p>
 * The following metadata elements will change behaviour.
 * <ul>
 * <li>emailsubject - Override the configured subject with the value stored against this key.
 * <li>emailcc - If this is set, this this comma separated list will override any configured CC list.</li>
 * </ul>
 * <p>
 * It is possible to control the underlying behaviour of this producer through the use of various properties that will be passed to
 * the <code>javax.mail.Session</code> instance. You need to refer to the javamail documentation to see a list of the available
 * properties and meanings.
 * </p>
 * <p>
 * You may also control the headers stored in the smtp client object. These are typically copied from the
 * <code>AdaptrisMessage</code>, which you can filter so as only to copy a subset of the metadata values as headers. There are 2
 * ways to do this; 1) Set the send-metadata-regexp value to a regular expression. 2) Set the metadata-filter to an implementation
 * of {@link MetadataFilter} In either case the metadata from the AdaptrisMessage will be filtered to a subset before copied as
 * headers.
 * </p>
 *
 * @see CoreConstants#EMAIL_SUBJECT
 * @see CoreConstants#EMAIL_CC_LIST
 */
public abstract class MailProducer extends ProduceOnlyProducerImp {

  @NotBlank
  private String smtpUrl = null;
  @InputFieldHint(expression = true)
  private String subject = null;
  @AdvancedConfig
  @InputFieldHint(expression = true)
  private String ccList = null;
  private String from = null;
  @AdvancedConfig
  @InputFieldHint(expression = true)
  private String bccList = null;
  @NotNull
  @Valid
  @AutoPopulated
  @AdvancedConfig
  private KeyValuePairSet sessionProperties;
  @NotNull
  @AutoPopulated
  @Valid
  @AdvancedConfig
  private MetadataFilter metadataFilter;
  @InputFieldHint(style = "PASSWORD", external = true)
  private String password;
  private String username;

  /**
   * The destination is a comma separated list of {@code TO} addresses.
   *
   */
  @Getter
  @Setter
  @Deprecated
  @Valid
  @Removal(version = "4.0.0", message = "Use 'to' instead")
  private ProduceDestination destination;

  /**
   * Comma separated list of email addresses to send to.
   */
  @InputFieldHint(expression = true)
  @Getter
  @Setter
  // Needs to be @NotBlank when destination is removed.
  private String to;

  private transient boolean destWarning;
  public MailProducer() {
    sessionProperties = new KeyValuePairSet();
    setMetadataFilter(new RemoveAllMetadataFilter());
  }

  /** @see com.adaptris.core.AdaptrisComponent#init() */
  @Override
  public void init() throws CoreException {
    try {
      Args.notNull(getSmtpUrl(), "smtpUrl");
      if (getSubject() == null) {
        log.warn("No Subject configured, expecting metadata to override subject");
      }
    }
    catch (Exception e) {
      throw ExceptionHelper.wrapCoreException(e);
    }
  }

  @Override
  public void prepare() throws CoreException {
    DestinationHelper.logWarningIfNotNull(destWarning, () -> destWarning = true, getDestination(),
        "{} uses destination, use 'to' instead", LoggingHelper.friendlyName(this));
    // To is optional if you want to just use bcc !.
    // DestinationHelper.mustHaveEither(getPath(), getDestination());

    registerEncoderMessageFactory();
  }

  /**
   * Set the SMTP url.
   *
   * @param url the url e.g. smtp://localhost:25/
   */
  public void setSmtpUrl(String url) {
    smtpUrl = url;
  }

  /**
   * Get the SMTP url.
   *
   * @return the url.
   */
  public String getSmtpUrl() {
    return smtpUrl;
  }

  /**
   * Set the list of CCs.
   *
   * @param ccList a comma separated list of CC addresses
   */
  public void setCcList(String ccList) {
    this.ccList = ccList;
  }

  /**
   * Get the list of CC's.
   *
   * @return the list of CC's
   */
  public String getCcList() {
    return ccList;
  }

  /**
   * Set the subject for the email.
   *
   * @param s the subject.
   */
  public void setSubject(String s) {
    subject = s;
  }

  /**
   * Get the subject of the email.
   *
   * @return the subject.
   */
  public String getSubject() {
    return subject;
  }

  /**
   * @deprecated since 3.10.0, slated for removal in 3.11.0, use message resolver instead.
   */
  @Deprecated
  private String getSubject(AdaptrisMessage msg) {
    return msg.containsKey(EmailConstants.EMAIL_SUBJECT) ? msg.getMetadataValue(EmailConstants.EMAIL_SUBJECT) : msg.resolve(getSubject());
  }

  /**
   * @deprecated since 3.10.0, slated for removal in 3.11.0, use message resolver instead.
   */
  @Deprecated
  private String getCC(AdaptrisMessage msg) {
    return msg.containsKey(EmailConstants.EMAIL_CC_LIST) ? msg.getMetadataValue(EmailConstants.EMAIL_CC_LIST) : msg.resolve(getCcList());
  }

  protected SmtpClient getClient(AdaptrisMessage msg) throws MailException, PasswordException {
    SmtpClient smtp = new SmtpClient(
        MailHelper.createURLName(getSmtpUrl(), getUsername(), ExternalResolver.resolve(getPassword())));
    Iterator i = sessionProperties.getKeyValuePairs().iterator();
    while (i.hasNext()) {
      KeyValuePair kp = (KeyValuePair) i.next();
      smtp.addSessionProperty(kp.getKey(), kp.getValue());
    }
    smtp.startSession();
    smtp.setSubject(getSubject(msg));
    String ccList = getCC(msg);
    if (ccList != null) {
      smtp.addCarbonCopy(ccList);
    }
    String bccList = getBccList();
    if (!StringUtils.isEmpty(bccList)) {
      smtp.addBlindCarbonCopy(msg.resolve(bccList));
    }
    if (getFrom() != null) {
      smtp.setFrom(getFrom());
    }

    MetadataCollection metadataSubset = getMetadataFilter().filter(msg);
    for (MetadataElement element : metadataSubset) {
      smtp.addMailHeader(element.getKey(), element.getValue());
    }
    return smtp;
  }

  /**
   * Set the from address.
   *
   * @param fromAddress the from address
   */
  public void setFrom(String fromAddress) {
    from = fromAddress;
  }

  /**
   * Get the from address.
   *
   * @return the from address.
   */
  public String getFrom() {
    return from;
  }

  /**
   * Get the session properties used by this SMTP Producer.
   *
   * @return the properties.
   */
  public KeyValuePairSet getSessionProperties() {
    return sessionProperties;
  }

  /**
   * Set the session properties to this SMTP Producer.
   *
   * @param kp the properties
   */
  public void setSessionProperties(KeyValuePairSet kp) {
    sessionProperties = Args.notNull(kp, "sessionProperties");
  }

  /**
   * @return the bccList
   */
  public String getBccList() {
    return bccList;
  }

  /**
   * Comma separated list of email addresses to BCC.
   *
   * @param bcc the bccList to set
   */
  public void setBccList(String bcc) {
    bccList = bcc;
  }

  public MetadataFilter getMetadataFilter() {
    return metadataFilter;
  }

  /**
   * Specify the {@link com.adaptris.core.AdaptrisMessage} metadata keys that will be sent as headers for the mail message.
   * <p>
   * Any metadata that is returned by this filter will be sent as headers.
   * </p>
   *
   * @param metadataFilter the filter defaults to {@link RemoveAllMetadataFilter}
   * @see MetadataFilter
   * @since 3.0.2
   */
  public void setMetadataFilter(MetadataFilter metadataFilter) {
    this.metadataFilter = Args.notNull(metadataFilter, "metadataFilter");
  }

  public String getPassword() {
    return password;
  }

  /**
   * Set the password to be used with this producer implementation.
   * <p>
   * If you specify the username and password in the URL for the SMTP server then does not lend itself to being encrypted. Specify
   * the password here if you wish to use {@link com.adaptris.security.password.Password#decode(String)} to decode the password.
   * </p>
   *
   * @param pw the password which will be overriden if a password is present in the URL.
   */
  public void setPassword(String pw) {
    password = pw;
  }

  public String getUsername() {
    return username;
  }

  /**
   * Set the username to be used with this producer implementation.
   *
   * @param name the username which will be overriden if a username is present in the URL.
   */
  public void setUsername(String name) {
    username = name;
  }

  @Override
  public String endpoint(AdaptrisMessage msg) throws ProduceException {
    return DestinationHelper.resolveProduceDestination(getTo(), getDestination(), msg);
  }

}
