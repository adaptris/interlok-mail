package com.adaptris.core.mail;

import static com.adaptris.core.mail.SendEmail.resolve;
import java.nio.charset.StandardCharsets;
import javax.validation.Valid;
import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.annotation.InputFieldDefault;
import com.adaptris.annotation.InputFieldHint;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageEncoder;
import com.adaptris.core.NullConnection;
import com.adaptris.core.ProduceException;
import com.adaptris.core.util.ExceptionHelper;
import com.adaptris.interlok.config.DataInputParameter;
import com.adaptris.mail.SmtpClient;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Replacement for {@link DefaultSmtpProducer} if you want to send an email with the payload as an
 * attachment.
 * <p>
 * Note that this producer will ignore any configured {@link AdaptrisMessageEncoder}. The template
 * if specified will be assumed to be plain text.
 * </p>
 *
 */
@XStreamAlias("send-email-attachment")
@AdapterComponent
@ComponentProfile(summary = "Send an email", tag = "email,smtp,mail",
    recommended = {NullConnection.class})
@DisplayOrder(order = {"to", "from", "subject", "body", "ccList", "bccList", "smtpUrl", "username",
    "password", "contentType", "contentEncoding"})
@NoArgsConstructor
public class SendEmailAttachment extends MailProducer {

  private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

  /**
   * The Content-Type of the email that will be sent.
   * <p>
   * The Content-Type may be any arbitary string such as application/edi-x12, however if no
   * appropriate {@code DataContentHandler} is installed, then the results can be undefined. It
   * defaults to {@code application/octet-stream} if not specified.
   * </p>
   */
  @InputFieldDefault(value = DEFAULT_CONTENT_TYPE)
  @Getter
  @Setter
  @InputFieldHint(expression = true)
  private String contentType = DEFAULT_CONTENT_TYPE;
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
  @InputFieldDefault(value = "base64")
  @InputFieldHint(expression = true)
  @Getter
  @Setter
  private String contentEncoding = null;

  /**
   * If specified, then this will be used as the filename for the attachment
   * <p>
   * Defaults to the message unique id if not explicitly specified.
   * </p>
   */
  @AdvancedConfig
  @InputFieldHint(expression = true)
  @Getter
  @Setter
  private String filename = null;

  /**
   * What is going to be the body of your message since the payload will be an attachment?
   * <p>
   * The template if specified will be assumed to be plain text
   * </p>
   */
  @Getter
  @Setter
  @Valid
  private DataInputParameter<String> body;

  @Override
  protected void doProduce(AdaptrisMessage msg, String toAddresses) throws ProduceException {
    try {
      SmtpClient smtp = getClient(msg, toAddresses);
      if (getBody() != null) {
        smtp.setMessage(msg.resolve(getBody().extract(msg), true).getBytes(StandardCharsets.UTF_8));
      }
      String filename = resolve(msg, getFilename(), msg.getUniqueId());
      String contentType = resolve(msg, getContentType(), DEFAULT_CONTENT_TYPE);
      String contentEncoding = resolve(msg, getContentEncoding(), "base64");
      smtp.addAttachment(encode(msg), filename, contentType, contentEncoding);
      smtp.send();
    } catch (Exception e) {
      throw ExceptionHelper.wrapProduceException(e);
    }
  }

}
