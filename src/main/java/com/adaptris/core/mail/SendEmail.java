package com.adaptris.core.mail;

import org.apache.commons.lang3.StringUtils;
import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.annotation.InputFieldDefault;
import com.adaptris.annotation.InputFieldHint;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.NullConnection;
import com.adaptris.core.ProduceException;
import com.adaptris.core.util.ExceptionHelper;
import com.adaptris.mail.SmtpClient;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Replaces DefaultSmtpProducer if you just want to send an email.
 */
@XStreamAlias("send-email")
@AdapterComponent
@ComponentProfile(summary = "Send an email", tag = "email,smtp,mail",
    recommended = {NullConnection.class})
@DisplayOrder(
    order = {"to", "from", "subject", "ccList", "bccList", "smtpUrl", "username", "password",
        "contentType", "contentEncoding"})
@NoArgsConstructor
public class SendEmail extends MailProducer {

  /**
   * The Content-Type of the email that will be sent.
   * <p>
   * The Content-Type may be any arbitary string such as application/edi-x12, however if no
   * appropriate {@code DataContentHandler} is installed, then the results can be undefined. It
   * defaults to {@code text/plain} if not specified.
   * </p>
   */
  @AdvancedConfig
  @Getter
  @Setter
  @InputFieldHint(expression = true)
  @InputFieldDefault(value = EmailConstants.TEXT_PLAIN)
  private String contentType;
  /**
   * The encoding of the email that will be sent.
   * <p>
   * Available Content-Encoding schemes that are supported are the same as those specified in
   * RFC2045 or those supported by jakarta mail. They include {@code base64, quoted-printable, 7bit,
   * 8bit and binary}. The default is {@code base64} if not otherwise specified to avoid encoding
   * issues.
   * </p>
   */
  @AdvancedConfig
  @InputFieldHint(expression = true)
  @InputFieldDefault(value = "base64")
  @Getter
  @Setter
  private String contentEncoding = null;

  @Override
  protected void doProduce(AdaptrisMessage msg, String toAddresses) throws ProduceException {
    try {
      SmtpClient smtp = getClient(msg, toAddresses);
      smtp.setEncoding(resolve(msg, getContentEncoding(), "base64"));
      smtp.setMessage(encode(msg), resolve(msg, getContentType(), EmailConstants.TEXT_PLAIN));
      smtp.send();
    } catch (Exception e) {
      throw ExceptionHelper.wrapProduceException(e);
    }
  }

  protected static String resolve(AdaptrisMessage msg, String configured, String defaultValue) {
    String result = StringUtils.defaultIfBlank(configured, defaultValue);
    return msg.resolve(result);
  }

}
