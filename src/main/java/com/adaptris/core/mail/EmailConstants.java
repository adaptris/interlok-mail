package com.adaptris.core.mail;

/**
 * @deprecated since 3.10.0, slated for removal in 3.11.0.
 */
@Deprecated
public class EmailConstants {

  /**
   * Metadata key specifying the email subject.
   * 
   * @deprecated since 3.10.0, slated for removal in 3.11.0.
   */
  @Deprecated
  public static final String EMAIL_SUBJECT = "emailsubject";

  /**
   * Metadata key that specifies the name of the attachment.
   *
   * @deprecated since 3.10.0, slated for removal in 3.11.0.
   */
  @Deprecated
  public static final String EMAIL_ATTACH_FILENAME = "emailattachmentfilename";

  /**
   * Metadata key that specifies the content-type of the attachment.
   *
   * @deprecated since 3.10.0, slated for removal in 3.11.0.
   */
  @Deprecated
  public static final String EMAIL_ATTACH_CONTENT_TYPE = "emailattachment" + "contenttype";

  /**
   * Metadata key that specifies the total number of attachments.
   *
   * @deprecated since 3.10.0, slated for removal in 3.11.0.
   */
  @Deprecated
  public static final String EMAIL_TOTAL_ATTACHMENTS = "emailtotalattachments";

  /**
   * Metadata key that specifies the cc list for sending.
   *
   * @deprecated since 3.10.0, slated for removal in 3.11.0.
   */
  @Deprecated
  public static final String EMAIL_CC_LIST = "emailcc";

  /**
   * Metadata key specifying the email message container from whence a payload may have spawned.
   *
   * @deprecated since 3.10.0, slated for removal in 3.11.0.
   */
  @Deprecated
  public static final String EMAIL_MESSAGE_ID = "emailmessageid";

  /**
   * Metadata key specifying the email body. This is only used if the <code>SmtpProducer</code> is configured to send the document
   * as an attachment.
   *
   * @deprecated since 3.10.0, slated for removal in 3.11.0.
   */
  @Deprecated
  public static final String EMAIL_TEMPLATE_BODY = "emailtemplatebody";
}
