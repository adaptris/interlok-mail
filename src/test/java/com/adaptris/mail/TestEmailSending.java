/*
 * Copyright 2015 Adaptris Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.adaptris.mail;

import static com.adaptris.mail.JunitMailHelper.DEFAULT_PAYLOAD_HTML;
import static com.adaptris.mail.JunitMailHelper.DEFAULT_RECEIVER;
import static com.adaptris.mail.JunitMailHelper.DEFAULT_SENDER;
import static com.adaptris.mail.JunitMailHelper.XML_DOCUMENT;
import static com.adaptris.mail.JunitMailHelper.createClient;
import static com.adaptris.mail.JunitMailHelper.testsEnabled;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import javax.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.icegreen.greenmail.util.GreenMail;

public class TestEmailSending {

  private static GreenMail greenmail;

  @BeforeAll
  public static void setupGreenmail() throws Exception {
    greenmail = JunitMailHelper.startServer();
  }

  @AfterAll
  public static void tearDownGreenmail() throws Exception {
    JunitMailHelper.stopServer(greenmail);
  }

  @BeforeEach
  public void before() throws Exception {
    assumeTrue(testsEnabled());
    greenmail.purgeEmailFromAllMailboxes();
  }


  @Test
  public void testSmtpSend() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    smtp.send();
    greenmail.waitForIncomingEmail(1);
    MimeMessage[] msgs = greenmail.getReceivedMessages();
    assertEquals(1, msgs.length);
    JunitMailHelper.assertFrom(msgs[0], DEFAULT_SENDER);
    JunitMailHelper.assertTo(msgs[0], DEFAULT_RECEIVER);
  }

  @Test
  public void testSmtpSendBase64Encoded() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    smtp.setEncoding("base64");
    smtp.send();
    greenmail.waitForIncomingEmail(1);
    MimeMessage[] msgs = greenmail.getReceivedMessages();
    assertEquals(1, msgs.length);
    JunitMailHelper.assertFrom(msgs[0], DEFAULT_SENDER);
    JunitMailHelper.assertTo(msgs[0], DEFAULT_RECEIVER);
  }

  @Test
  public void testSmtpSendWithContentType() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    smtp.setMessage(DEFAULT_PAYLOAD_HTML.getBytes(), "text/html");
    smtp.send();
    greenmail.waitForIncomingEmail(1);
    MimeMessage[] msgs = greenmail.getReceivedMessages();
    assertEquals(1, msgs.length);
    JunitMailHelper.assertFrom(msgs[0], DEFAULT_SENDER);
    JunitMailHelper.assertTo(msgs[0], DEFAULT_RECEIVER);
  }

  @Test
  public void testSmtpSendAttachment() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream print = new PrintStream(out);
    print.print(XML_DOCUMENT);
    print.close();
    out.close();
    smtp.addAttachment(out.toByteArray(), "filename.xml", null);
    smtp.send();
    greenmail.waitForIncomingEmail(1);
    MimeMessage[] msgs = greenmail.getReceivedMessages();
    assertEquals(1, msgs.length);
    JunitMailHelper.assertFrom(msgs[0], DEFAULT_SENDER);
    JunitMailHelper.assertTo(msgs[0], DEFAULT_RECEIVER);
  }

  @Test
  public void testSmtpSendAttachmentBase64Encoded() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream print = new PrintStream(out);
    print.print(XML_DOCUMENT);
    print.close();
    out.close();
    smtp.addAttachment(out.toByteArray(), "filename.xml", "text/plain");
    smtp.setEncoding("base64");
    smtp.send();
    greenmail.waitForIncomingEmail(1);
    MimeMessage[] msgs = greenmail.getReceivedMessages();
    assertEquals(1, msgs.length);
    JunitMailHelper.assertFrom(msgs[0], DEFAULT_SENDER);
    JunitMailHelper.assertTo(msgs[0], DEFAULT_RECEIVER);
  }

  @Test
  public void testSmtpSendAttachmentUUEncoded() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream print = new PrintStream(out);
    print.print(XML_DOCUMENT);
    print.close();
    out.close();
    smtp.addAttachment(out.toByteArray(), "filename.xml", null);
    smtp.setEncoding("uuencode");
    smtp.send();
    greenmail.waitForIncomingEmail(1);
    MimeMessage[] msgs = greenmail.getReceivedMessages();
    assertEquals(1, msgs.length);
    JunitMailHelper.assertFrom(msgs[0], DEFAULT_SENDER);
    JunitMailHelper.assertTo(msgs[0], DEFAULT_RECEIVER);
  }
}
