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

import static com.adaptris.mail.JunitMailHelper.testsEnabled;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import jakarta.mail.URLName;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.icegreen.greenmail.smtp.SmtpServer;
import com.icegreen.greenmail.util.GreenMail;

public class TestSmtpClient {

  private static final String INVALID_EMAIL_ADDR = "a\"b(c)d,e:f;g<h>i[j\\k]l@example.com";
  private static final String DUMMY_VALUE = "HELO";
  private static final String DUMMY_KEY = "EHLO";

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
  public void testConstructors() throws Exception {
    SmtpServer server = greenmail.getSmtp();
    String smtpUrl = server.getProtocol() + "://localhost:" + server.getPort();
    new SmtpClient(smtpUrl);
    new SmtpClient(new URLName(smtpUrl));
    new SmtpClient("localhost", server.getPort(), "", "");
  }

  @Test
  public void testAddSessionProperties() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    smtp.addSessionProperty(DUMMY_KEY, DUMMY_VALUE);
    assertTrue(smtp.getSessionProperties().containsKey(DUMMY_KEY));
    assertEquals(DUMMY_VALUE, smtp.getSessionProperties().getProperty(DUMMY_KEY));
    smtp.removeSessionProperty(DUMMY_KEY);
    assertFalse(smtp.getSessionProperties().containsKey(DUMMY_KEY));
    smtp.removeSessionProperty(DUMMY_KEY);
    assertFalse(smtp.getSessionProperties().containsKey(DUMMY_KEY));
  }

  @Test
  public void testStartSession() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    assertNull(smtp.session);
    smtp.startSession();
    assertNotNull(smtp.session);
    assertNotNull(smtp.message);
  }

  @Test
  public void testNewMessage_WithoutStartSession() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    assertThrows(MailException.class, ()->{
      smtp.newMessage();
    }, "Failed creating a new message, no session");
  }

  @Test
  public void testAddMailHeader() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    smtp.startSession();
    smtp.addMailHeader(DUMMY_KEY, DUMMY_VALUE);
    assertNotNull(smtp.message.getHeader(DUMMY_KEY));
    assertEquals(DUMMY_VALUE, smtp.message.getHeader(DUMMY_KEY)[0]);
  }

  @Test
  public void testRemoveMailHeader() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    smtp.startSession();
    smtp.addMailHeader(DUMMY_KEY, DUMMY_VALUE);
    assertNotNull(smtp.message.getHeader(DUMMY_KEY));
    assertEquals(DUMMY_VALUE, smtp.message.getHeader(DUMMY_KEY)[0]);
    smtp.removeMailHeader(DUMMY_KEY);
    assertNull(smtp.message.getHeader(DUMMY_KEY));
  }

  @Test
  public void testAddTo_NoSession() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    assertThrows(MailException.class, ()->{
      smtp.addTo("abc@adaptris.com, bcd@adaptris.com, def@adaptris.com");
    }, "Failed addTo, no session");
  }

  @Test
  public void testAddTo() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    smtp.startSession();
    smtp.addTo("abc@adaptris.com, bcd@adaptris.com, def@adaptris.com");
  }

  @Test
  public void testAddTo_Invalid() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    smtp.startSession();
    assertThrows(MailException.class, ()->{
      smtp.addTo(INVALID_EMAIL_ADDR);
    }, "Failed addTo, invalid email address");
  }

  @Test
  public void testAddCc_NoSession() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    assertThrows(MailException.class, ()->{
      smtp.addCarbonCopy("abc@adaptris.com, bcd@adaptris.com, def@adaptris.com");
    }, "Failed addCc, no session");
  }

  @Test
  public void testAddCc() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    smtp.startSession();
    smtp.addCarbonCopy("abc@adaptris.com, bcd@adaptris.com, def@adaptris.com");
  }

  @Test
  public void testAddCc_Invalid() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    smtp.startSession();
    assertThrows(MailException.class, ()->{
      smtp.addCarbonCopy(INVALID_EMAIL_ADDR);
    }, "Failed addCc, invalid email address");
  }

  @Test
  public void testAddBcc_NoSession() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    assertThrows(MailException.class, ()->{
      smtp.addBlindCarbonCopy("abc@adaptris.com, bcd@adaptris.com, def@adaptris.com");
    }, "Failed addBcc, no session");
  }

  @Test
  public void testAddBcc() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    smtp.startSession();
    smtp.addBlindCarbonCopy("abc@adaptris.com, bcd@adaptris.com, def@adaptris.com");
  }

  @Test
  public void testAddBcc_Invalid() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    smtp.startSession();
    assertThrows(MailException.class, ()->{
      smtp.addBlindCarbonCopy(INVALID_EMAIL_ADDR);
    }, "Failed addBcc, invalid email address");
  }

  @Test
  public void testSetFrom_NoSession() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    assertThrows(MailException.class, ()->{
      smtp.setFrom("abc@adaptris.com");
    }, "Failed setFrom, no session");
  }

  @Test
  public void testSetFrom() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    smtp.startSession();
    smtp.setFrom("abc@adaptris.com");
  }

  @Test
  public void testAddFrom_Invalid() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    smtp.startSession();
    assertThrows(MailException.class, ()->{
      smtp.setFrom(INVALID_EMAIL_ADDR);
    }, "Failed setFrom, invalid email address");
  }

  @Test
  public void testSetSubject_NoSession() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    assertThrows(MailException.class, ()->{
      smtp.setSubject("abc@adaptris.com");
    }, "Failed setSubject, no session");
  }

  @Test
  public void testSetSubject() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    smtp.startSession();
    smtp.setSubject("abc@adaptris.com");
  }

  @Test
  public void testSend_WithoutStartSession() throws Exception {
    assertThrows(MailException.class, ()->{
      SmtpClient smtp = createClient(greenmail);
      smtp.setMessage(JunitMailHelper.DEFAULT_PAYLOAD.getBytes());
      smtp.setSubject(JunitMailHelper.DEFAULT_SUBJECT);
      smtp.addTo(JunitMailHelper.DEFAULT_RECEIVER);
      smtp.send();
    }, "Failed to send, no session");
  }

  @Test
  public void testSend_NoFromAddress() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    smtp.startSession();
    smtp.setMessage(JunitMailHelper.DEFAULT_PAYLOAD.getBytes());
    smtp.setSubject(JunitMailHelper.DEFAULT_SUBJECT);
    smtp.addTo(JunitMailHelper.DEFAULT_RECEIVER);
    smtp.send();
    greenmail.waitForIncomingEmail(1);
    MimeMessage[] msgs = greenmail.getReceivedMessages();
    assertEquals(1, msgs.length);
    assertNotNull(msgs[0].getFrom());
  }

  @Test
  public void testSend_NoRecipients() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    smtp.startSession();
    smtp.setMessage(JunitMailHelper.DEFAULT_PAYLOAD.getBytes());
    smtp.setSubject(JunitMailHelper.DEFAULT_SUBJECT);
    assertThrows(MailException.class, ()->{
      smtp.send();
    }, "Failed to send, no recipients set");
  }

  private static SmtpClient createClient(GreenMail gm) throws Exception {
    SmtpServer server = gm.getSmtp();
    String smtpUrl = server.getProtocol() + "://localhost:" + server.getPort();
    SmtpClient client = new SmtpClient(smtpUrl);
    return client;
  }

}
