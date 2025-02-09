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

package com.adaptris.mail;

import jakarta.mail.Transport;
import jakarta.mail.URLName;
import jakarta.mail.event.TransportEvent;
import jakarta.mail.event.TransportListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adaptris.core.mail.URLNameHelper;

/**
 * The Adaptris SMTP client.
 * <p>
 * This implementation is a wrapper around the Javamail api, and supports the
 * SMTP Protocol
 * <p>
 * The message is sent as a multipart mime message with optional encoding.
 * </p>
 * <p>
 * Available Content-Encoding schemes that are supported are the same as those
 * specified in RFC2045. They include "base64", "quoted-printable", "7bit",
 * "8bit" and "binary
 * </p>
 * <p>
 * The Content-Type may be any arbitary string such as application/edi-x12,
 * however if no appropriate <code>DataContentHandler</code> is installed,
 * then the results can be undefined
 * </p>
 */
public final class SmtpClient extends MailSenderImp implements TransportListener {

  private URLName smtpUrl;
  private transient Logger log = LoggerFactory.getLogger(this.getClass());

  /** Constructor */
  private SmtpClient() {
    super();
  }

  /**
   * Constructor.
   *
   * @param url the SMTP host to forward requests to in the format
   *          smtp://user:pw@hostname:port/
   */
  public SmtpClient(String url) {
    this(new URLName(url));
  }

  /**
   * Constructor.
   *
   * @param url the SMTP host to forward requests to in the format
   *          smtp://user:pw@hostname:port/
   */
  public SmtpClient(URLName url) {
    this();
    smtpUrl = url;
  }

  /**
   * Constructor.
   * <p>
   * SMTP is the implied protocol, and there is no "File" field for the URLName
   * </p>
   *
   * @param host the hostname
   * @param port the port number
   * @param user the username
   * @param pw the password
   */
  public SmtpClient(String host, int port, String user, String pw) {
    this(new URLName("smtp", host, port, "", user, pw));
  }

  /**
   * Send the email.
   * <p>
   * No validation of the email addresses is done, an incorrect email address
   * causes a MailException to be thrown The message IS sent to valid addresses.
   *
   * @throws MailException wrapping any underlying exception
   */
  @Override
  public void send() throws MailException {
    buildContent();

    try {
      Transport tr = session.getTransport(smtpUrl);
      log.trace("Attempting to send {} using SMTP {}", message.getMessageID(), URLNameHelper.toSafeString(smtpUrl));
      if (smtpUrl.getUsername() != null) {
        tr.connect(smtpUrl.getHost(), smtpUrl.getPort(), smtpUrl.getUsername(),
            smtpUrl.getPassword());
      }
      else {
        tr.connect();
      }
      tr.sendMessage(message, message.getAllRecipients());
      tr.close();
    }
    catch (Exception e) {
      // logR.trace("Failed to send email ", e);
      throw new MailException(e);
    }
  }

  /**
   * @see TransportListener#messageDelivered(TransportEvent)
   */
  @Override
  public void messageDelivered(TransportEvent e) {
    log.debug("Message was successfully delivered");
  }

  /**
   * @see TransportListener#messageNotDelivered(TransportEvent)
   */
  @Override
  public void messageNotDelivered(TransportEvent e) {
    log.error("Message was NOT successfully delivered");
  }

  /**
   * @see TransportListener#messagePartiallyDelivered(TransportEvent)
   */
  @Override
  public void messagePartiallyDelivered(TransportEvent e) {
    log.warn("Message was partially delivered");
  }
}
