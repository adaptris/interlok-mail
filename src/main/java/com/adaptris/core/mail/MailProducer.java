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
import com.adaptris.annotation.InputFieldHint;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.CoreException;
import com.adaptris.core.MetadataCollection;
import com.adaptris.core.MetadataElement;
import com.adaptris.core.ProduceException;
import com.adaptris.core.ProduceOnlyProducerImp;
import com.adaptris.core.metadata.MetadataFilter;
import com.adaptris.core.metadata.RemoveAllMetadataFilter;
import com.adaptris.core.util.Args;
import com.adaptris.interlok.resolver.ExternalResolver;
import com.adaptris.mail.MailException;
import com.adaptris.mail.SmtpClient;
import com.adaptris.security.exc.PasswordException;
import com.adaptris.util.KeyValuePair;
import com.adaptris.util.KeyValuePairSet;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Abstract implementation of the AdaptrisMessageProducer interface for handling Email.
 * <p>
 * Because email is implicitly asynchronous, Request-Reply is invalid, and as such if the request method is used, an
 * <code>UnsupportedOperationException</code> is thrown.
 * </p>
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
 */
public abstract class MailProducer extends ProduceOnlyProducerImp {

  /**
   * The SMTP Server.
   *
   */
  @NotBlank
  @Getter
  @Setter
  @NonNull
  private String smtpUrl = null;
  /**
   * The subject for the email.
   *
   */
  @InputFieldHint(expression = true)
  @Getter
  @Setter
  private String subject = null;
  /**
   * A comma separated list of email addresses to 'cc'
   *
   */
  @Getter
  @Setter
  @AdvancedConfig
  @InputFieldHint(expression = true)
  private String ccList = null;
  /**
   * The 'From' Address
   *
   */
  @Getter
  @Setter
  @InputFieldHint(expression = true)
  private String from = null;
  /**
   * A comma separated list of email addresses to 'bcc'
   *
   */
  @Getter
  @Setter
  @AdvancedConfig
  @InputFieldHint(expression = true)
  private String bccList = null;
  /**
   * Any additional behaviour that should be applied to the underlying {@code javax.mail.Session}.
   * <p>
   * You probably need to consult the javamail documentation to find the exhaustive list of
   * properties.
   * </p>
   */
  @Getter
  @Setter
  @NotNull
  @Valid
  @AutoPopulated
  @AdvancedConfig
  private KeyValuePairSet sessionProperties;

  /**
   * Specify the {@link com.adaptris.core.AdaptrisMessage} metadata keys that will be sent as
   * headers for the mail message.
   * <p>
   * Any metadata that is returned by this filter will be sent as headers. It defaults to
   * {@link RemoveAllMetadataFilter} if not specified.
   * </p>
   */
  @Getter
  @Setter
  @NotNull
  @AutoPopulated
  @Valid
  @AdvancedConfig
  @NonNull
  private MetadataFilter metadataFilter;
  /**
   * The password associated with the SMTP Server
   * <p>
   * If you specify the username and password in the URL for the SMTP server then does not lend
   * itself to being encrypted. Specify the password here if you wish to use
   * {@link com.adaptris.security.password.Password#decode(String)} to decode the password.
   * </p>
   */
  @Getter
  @Setter
  @InputFieldHint(style = "PASSWORD", external = true)
  private String password;
  /**
   * Set an optional username for the SMTP Server.
   * <p>
   * The username which will be overriden if a username is present in the URL
   * </p>
   */
  @Getter
  @Setter
  private String username;

  /**
   * Comma separated list of email addresses to send to.
   */
  @InputFieldHint(expression = true)
  @Getter
  @Setter
  @NotBlank
  private String to;

  public MailProducer() {
    sessionProperties = new KeyValuePairSet();
    setMetadataFilter(new RemoveAllMetadataFilter());
  }

  @Override
  public void prepare() throws CoreException {
    Args.notNull(getSmtpUrl(), "smtpUrl");
    // To is optional if you want to just use bcc !.
    // DestinationHelper.mustHaveEither(getPath(), getDestination());

    registerEncoderMessageFactory();
  }

  protected SmtpClient getClient(AdaptrisMessage msg, String toAddresses)
      throws MailException, PasswordException {
    SmtpClient smtp = getClient(msg);
    if (!StringUtils.isEmpty(toAddresses)) {
      smtp.addTo(toAddresses);
    }
    return smtp;
  }


  protected SmtpClient getClient(AdaptrisMessage msg) throws MailException, PasswordException {
    SmtpClient smtp = new SmtpClient(
        MailHelper.createURLName(getSmtpUrl(), getUsername(), ExternalResolver.resolve(getPassword())));
    for (KeyValuePair kp : getSessionProperties()) {
      smtp.addSessionProperty(kp.getKey(), kp.getValue());
    }
    smtp.startSession();
    smtp.setSubject(msg.resolve(getSubject()));
    String ccList = msg.resolve(getCcList());
    if (ccList != null) {
      smtp.addCarbonCopy(ccList);
    }
    String bccList = getBccList();
    if (!StringUtils.isEmpty(bccList)) {
      smtp.addBlindCarbonCopy(msg.resolve(bccList));
    }
    if (getFrom() != null) {
      smtp.setFrom(msg.resolve(getFrom()));
    }
    MetadataCollection metadataSubset = getMetadataFilter().filter(msg);
    for (MetadataElement element : metadataSubset) {
      smtp.addMailHeader(element.getKey(), element.getValue());
    }
    return smtp;
  }

  @Override
  public String endpoint(AdaptrisMessage msg) throws ProduceException {
    return msg.resolve(getTo());
  }

}
