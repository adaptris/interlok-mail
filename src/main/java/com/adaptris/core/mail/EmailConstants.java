package com.adaptris.core.mail;

public class EmailConstants {

  /**
   * Metadata key specifying the email subject.
   * 
   */
  public static final String EMAIL_SUBJECT = "emailsubject";

  /**
   * Metadata key that specifies the name of the attachment.
   * 
   */
  public static final String EMAIL_ATTACH_FILENAME = "emailattachmentfilename";

  /**
   * Metadata key that specifies the content-type of the attachment.
   * 
   */
  public static final String EMAIL_ATTACH_CONTENT_TYPE = "emailattachment" + "contenttype";

  /**
   * Metadata key that specifies the total number of attachments.
   * 
   */
  public static final String EMAIL_TOTAL_ATTACHMENTS = "emailtotalattachments";

  /**
   * Metadata key that specifies the cc list for sending.
   * 
   */
  public static final String EMAIL_CC_LIST = "emailcc";

  /**
   * Metadata key specifying the email message container from whence a payload may have spawned.
   * 
   */
  public static final String EMAIL_MESSAGE_ID = "emailmessageid";

  /**
   * Metadata key specifying the email body. This is only used if the <code>SmtpProducer</code> is configured to send the document
   * as an attachment.
   * 
   */
  public static final String EMAIL_TEMPLATE_BODY = "emailtemplatebody";

}
