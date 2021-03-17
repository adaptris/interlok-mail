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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import javax.mail.URLName;
import javax.mail.internet.MimeMessage;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import com.icegreen.greenmail.smtp.SmtpServer;
import com.icegreen.greenmail.util.GreenMail;

public class TestSmtpClient {

  private static final String INVALID_EMAIL_ADDR = "a\"b(c)d,e:f;g<h>i[j\\k]l@example.com";
  private static final String DUMMY_VALUE = "HELO";
  private static final String DUMMY_KEY = "EHLO";

  private static GreenMail greenmail;

  @BeforeClass
  public static void setupGreenmail() throws Exception {
    greenmail = JunitMailHelper.startServer();
  }

  @AfterClass
  public static void tearDownGreenmail() throws Exception {
    JunitMailHelper.stopServer(greenmail);
  }

  @Before
  public void before() throws Exception {
    Assume.assumeTrue(testsEnabled());
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

  @Test(expected = MailException.class)
  public void testNewMessage_WithoutStartSession() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    smtp.newMessage();
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

  @Test(expected = MailException.class)
  public void testAddTo_NoSession() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    smtp.addTo("abc@adaptris.com, bcd@adaptris.com, def@adaptris.com");
  }

  @Test
  public void testAddTo() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    smtp.startSession();
    smtp.addTo("abc@adaptris.com, bcd@adaptris.com, def@adaptris.com");
  }

  @Test(expected = MailException.class)
  public void testAddTo_Invalid() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    smtp.startSession();
    smtp.addTo(INVALID_EMAIL_ADDR);
  }

  @Test(expected = MailException.class)
  public void testAddCc_NoSession() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    smtp.addCarbonCopy("abc@adaptris.com, bcd@adaptris.com, def@adaptris.com");
  }

  @Test
  public void testAddCc() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    smtp.startSession();
    smtp.addCarbonCopy("abc@adaptris.com, bcd@adaptris.com, def@adaptris.com");
  }

  @Test(expected = MailException.class)
  public void testAddCc_Invalid() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    smtp.startSession();
    smtp.addCarbonCopy(INVALID_EMAIL_ADDR);
  }

  @Test(expected = MailException.class)
  public void testAddBcc_NoSession() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    smtp.addBlindCarbonCopy("abc@adaptris.com, bcd@adaptris.com, def@adaptris.com");
  }

  @Test
  public void testAddBcc() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    smtp.startSession();
    smtp.addBlindCarbonCopy("abc@adaptris.com, bcd@adaptris.com, def@adaptris.com");
  }

  @Test(expected = MailException.class)
  public void testAddBcc_Invalid() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    smtp.startSession();
    smtp.addBlindCarbonCopy(INVALID_EMAIL_ADDR);
  }

  @Test(expected = MailException.class)
  public void testSetFrom_NoSession() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    smtp.setFrom("abc@adaptris.com");
  }

  @Test
  public void testSetFrom() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    smtp.startSession();
    smtp.setFrom("abc@adaptris.com");
  }

  @Test(expected = MailException.class)
  public void testAddFrom_Invalid() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    smtp.startSession();
    smtp.setFrom(INVALID_EMAIL_ADDR);
  }

  @Test(expected = MailException.class)
  public void testSetSubject_NoSession() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    smtp.setSubject("abc@adaptris.com");
  }

  @Test
  public void testSetSubject() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    smtp.startSession();
    smtp.setSubject("abc@adaptris.com");
  }

  @Test(expected = MailException.class)
  public void testSend_WithoutStartSession() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    smtp.setMessage(JunitMailHelper.DEFAULT_PAYLOAD.getBytes());
    smtp.setSubject(JunitMailHelper.DEFAULT_SUBJECT);
    smtp.addTo(JunitMailHelper.DEFAULT_RECEIVER);
    smtp.send();
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

  @Test(expected = MailException.class)
  public void testSend_NoRecipients() throws Exception {
    SmtpClient smtp = createClient(greenmail);
    smtp.startSession();
    smtp.setMessage(JunitMailHelper.DEFAULT_PAYLOAD.getBytes());
    smtp.setSubject(JunitMailHelper.DEFAULT_SUBJECT);
    smtp.send();
  }

  private static SmtpClient createClient(GreenMail gm) throws Exception {
    SmtpServer server = gm.getSmtp();
    String smtpUrl = server.getProtocol() + "://localhost:" + server.getPort();
    SmtpClient client = new SmtpClient(smtpUrl);
    return client;
  }

}
