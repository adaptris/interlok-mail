package com.adaptris.core.mail;

import com.adaptris.annotation.Removal;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EmailConstants {

  /**
   * Metadata key specifying the email subject.
   *
   * @deprecated since 3.10.0, slated for removal in 4.0.
   */
  @Removal(message = "Use message resolver to reference metadata keys: %message{emailsubject}",
      version = "4.0")
  @Deprecated
  public static final String EMAIL_SUBJECT = "emailsubject";

  /**
   * Metadata key that specifies the name of the attachment.
   *
   * @deprecated since 3.10.0, slated for removal in 4.0.
   */
  @Removal(
      message = "Use message resolver to reference metadata keys: %message{emailattachmentfilename}",
      version = "4.0")
  @Deprecated
  public static final String EMAIL_ATTACH_FILENAME = "emailattachmentfilename";

  /**
   * Metadata key that specifies the content-type of the attachment.
   *
   * @deprecated since 3.10.0, slated for removal in 4.0.
   */
  @Removal(
      message = "Use message resolver to reference metadata keys: %message{emailattachmentcontenttype}",
      version = "4.0")
  @Deprecated
  public static final String EMAIL_ATTACH_CONTENT_TYPE = "emailattachmentcontenttype";

  /**
   * Metadata key that specifies the total number of attachments.
   *
   */
  public static final String EMAIL_TOTAL_ATTACHMENTS = "emailtotalattachments";

  /**
   * Metadata key that specifies the cc list for sending.
   *
   * @deprecated since 3.10.0, slated for removal in 4.0
   */
  @Removal(message = "Use message resolver to reference metadata keys: %message{emailcc}",
      version = "4.0")
  @Deprecated
  public static final String EMAIL_CC_LIST = "emailcc";

  /**
   * Metadata key specifying the email message container from whence a payload may have spawned.
   *
   */
  public static final String EMAIL_MESSAGE_ID = "emailmessageid";

  /**
   * Metadata key specifying the email body. This is only used if the <code>SmtpProducer</code> is
   * configured to send the document as an attachment.
   *
   * @deprecated since 3.10.0, slated for removal in 4.0
   */
  @Removal(message = "Use message resolver to reference metadata keys: %message{emailtemplatebody}",
      version = "4.0")
  @Deprecated
  public static final String EMAIL_TEMPLATE_BODY = "emailtemplatebody";

  public static final String TEXT_PLAIN = "text/plain";

}
