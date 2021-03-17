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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.CoreException;
import com.adaptris.core.NullConnection;
import com.adaptris.mail.Attachment;
import com.adaptris.mail.MailException;
import com.adaptris.mail.MessageParser;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Default Email implementation of the AdaptrisMessageConsumer interface.
 * <p>
 * Each Mime part of the incoming email message will become a separate AdaptrisMessage; attachments are processed separately from
 * the mail body itself.
 * </p>
 *
 *
 * @config default-mail-consumer
 *
 *
 * @see MailConsumerImp
 */
@XStreamAlias("default-mail-consumer")
@AdapterComponent
@ComponentProfile(summary = "Pickup messages from a email account parsing the MIME message", tag = "consumer,email", metadata =
{
    "emailmessageid", "emailtotalattachments", "emailattachmentfilename", "emailattachmentcontenttype"
},
    recommended = {NullConnection.class})
@DisplayOrder(order = {"mailboxUrl", "poller", "username", "password", "mailReceiverFactory",
    "partSelector", "headerHandler"})
public class DefaultMailConsumer extends ParsingMailConsumerImpl {

  /**
   * <p>
   * Creates a new instance.
   * </p>
   */
  public DefaultMailConsumer() {
  }

  @Override
  protected List<AdaptrisMessage> createMessages(MimeMessage mime) throws MailException, CoreException {

    List<AdaptrisMessage> result = new ArrayList<AdaptrisMessage>();
    try {
      MessageParser mp = new MessageParser(mime, getPartSelector());
      log.trace("Start Processing [{}]", mp.getMessageId());
      if (mp.getMessage() != null) {
        AdaptrisMessage msg = decode(mp.getMessage());
        if (mp.getMessageId() != null) {
          msg.addMetadata(EmailConstants.EMAIL_MESSAGE_ID, mp.getMessageId());
        }
        headerHandler().handle(mime, msg);
        if (mp.hasAttachments()) {
          msg.addMetadata(EmailConstants.EMAIL_TOTAL_ATTACHMENTS, String.valueOf(mp.numberOfAttachments()));
        }
        result.add(msg);
      }
      if (mp.hasAttachments()) {
        while (mp.hasMoreAttachments()) {
          Attachment a = mp.nextAttachment();
          AdaptrisMessage msg = decode(a.getBytes());
          msg.addMetadata(EmailConstants.EMAIL_MESSAGE_ID, mp.getMessageId());
          msg.addMetadata(EmailConstants.EMAIL_ATTACH_FILENAME, a.getFilename());
          msg.addMetadata(EmailConstants.EMAIL_ATTACH_CONTENT_TYPE, a.getContentType());
          headerHandler().handle(mime, msg);
          msg.addMetadata(EmailConstants.EMAIL_TOTAL_ATTACHMENTS, String.valueOf(mp.numberOfAttachments()));
          result.add(msg);
        }
      }
    }
    catch (MessagingException | IOException e) {
      throw new MailException(e);
    }
    return result;
  }

  @Override
  protected void initConsumer() throws CoreException {}

}
