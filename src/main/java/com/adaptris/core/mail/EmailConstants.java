package com.adaptris.core.mail;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EmailConstants {

  /**
   * Metadata key that specifies the name of the attachment.
   *
   */
  public static final String EMAIL_ATTACH_FILENAME = "emailattachmentfilename";

  /**
   * Metadata key that specifies the content-type of the attachment.
   *
   */
  public static final String EMAIL_ATTACH_CONTENT_TYPE = "emailattachmentcontenttype";

  /**
   * Metadata key that specifies the total number of attachments.
   *
   */
  public static final String EMAIL_TOTAL_ATTACHMENTS = "emailtotalattachments";

  /**
   * Metadata key specifying the email message container from whence a payload may have spawned.
   *
   */
  public static final String EMAIL_MESSAGE_ID = "emailmessageid";

  public static final String TEXT_PLAIN = "text/plain";

}
