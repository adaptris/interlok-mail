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

import static com.adaptris.mail.JunitMailHelper.DEFAULT_PAYLOAD;
import static com.adaptris.mail.JunitMailHelper.DEFAULT_RECEIVER;
import static com.adaptris.mail.JunitMailHelper.DEFAULT_SENDER;
import static com.adaptris.mail.JunitMailHelper.DEFAULT_SUBJECT;
import static com.adaptris.mail.JunitMailHelper.assertFrom;
import static com.adaptris.mail.JunitMailHelper.assertTo;
import static com.adaptris.mail.JunitMailHelper.startServer;
import static com.adaptris.mail.JunitMailHelper.stopServer;
import static com.adaptris.mail.JunitMailHelper.testsEnabled;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.junit.Assert.assertEquals;
import java.util.Enumeration;
import javax.mail.Header;
import javax.mail.URLName;
import javax.mail.internet.MimeMessage;
import org.apache.commons.io.IOUtils;
import org.junit.Assume;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.adaptris.core.BaseCase;
import com.adaptris.security.exc.PasswordException;
import com.adaptris.security.password.Password;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;

@SuppressWarnings("deprecation")
public abstract class MailReceiverCase extends BaseCase {


  protected static final String DEFAULT_POP3_USER = "junit";
  protected static final String DEFAULT_POP3_PASSWORD = "junit";
  protected static final String DEFAULT_ENCODED_POP3_PASSWORD;

  static {
    try {
      DEFAULT_ENCODED_POP3_PASSWORD = Password.encode(DEFAULT_POP3_PASSWORD, Password.PORTABLE_PASSWORD);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected Logger logger = LoggerFactory.getLogger(this.getClass());

  protected static final String JAVA_UTIL = "Regex";

  abstract MailReceiver createClient(GreenMail gm) throws Exception;

  @Test
  public void testConnect() throws Exception {
    Assume.assumeTrue(testsEnabled());
    GreenMail gm = startServer(DEFAULT_RECEIVER, DEFAULT_POP3_USER, DEFAULT_POP3_PASSWORD);
    try {
      MailReceiver mbox = createClient(gm);
      try {
        mbox.connect();
      }
      finally {
        IOUtils.closeQuietly(mbox);
      }

    }
    finally {
      stopServer(gm);
    }
  }

  protected void sendMessage(String from, String to, ServerSetup setup) throws Exception {
    GreenMailUtil.sendTextEmail(to, from, DEFAULT_SUBJECT, DEFAULT_PAYLOAD, setup);
  }

  protected void sendMessage(String from, String to, String subject, ServerSetup setup) throws Exception {
    GreenMailUtil.sendTextEmail(to, from, subject, DEFAULT_PAYLOAD, setup);
  }

  @Test
  public void testFilterNoMatch_WithDelete() throws Exception {
    Assume.assumeTrue(testsEnabled());
    String name = Thread.currentThread().getName();
    Thread.currentThread().setName(getName());
    GreenMail gm = startServer(DEFAULT_RECEIVER, DEFAULT_POP3_USER, DEFAULT_POP3_PASSWORD);
    ServerSetup smtpServerSetup = new ServerSetup(gm.getSmtp().getPort(), null, ServerSetup.PROTOCOL_SMTP);
    sendMessage("anotherAddress@anotherDomain.com", DEFAULT_RECEIVER, smtpServerSetup);
    sendMessage("anotherAddress@anotherDomain.com", DEFAULT_RECEIVER, smtpServerSetup);
    sendMessage("anotherAddress@anotherDomain.com", DEFAULT_RECEIVER, smtpServerSetup);
    sendMessage("anotherAddress@anotherDomain.com", DEFAULT_RECEIVER, smtpServerSetup);
    sendMessage("anotherAddress@anotherDomain.com", DEFAULT_RECEIVER, smtpServerSetup);
    MailReceiver mbox = createClient(gm);
    mbox.purge(true);
    mbox.setRegularExpressionCompiler(JAVA_UTIL);
    mbox.setSubjectFilter(".*ZZZZZZ.*");
    MailReceiver mboxChecker = createClient(gm);
    try {
      mbox.connect();
      assertEquals(0, mbox.getMessages().size());
      IOUtils.closeQuietly(mbox);
      mboxChecker.connect();
      assertEquals(5, mboxChecker.getMessages().size());
    }
    finally {
      IOUtils.closeQuietly(mbox);
      IOUtils.closeQuietly(mboxChecker);
      stopServer(gm);
      Thread.currentThread().setName(name);
    }
  }

  @Test
  public void testFilterMatch_WithDelete() throws Exception {
    Assume.assumeTrue(testsEnabled());
    String name = Thread.currentThread().getName();
    Thread.currentThread().setName(getName());
    GreenMail gm = startServer(DEFAULT_RECEIVER, DEFAULT_POP3_USER, DEFAULT_POP3_PASSWORD);
    ServerSetup smtpServerSetup = new ServerSetup(gm.getSmtp().getPort(), null, ServerSetup.PROTOCOL_SMTP);
    sendMessage("anotherAddress@anotherDomain.com", DEFAULT_RECEIVER, smtpServerSetup);
    sendMessage("anotherAddress@anotherDomain.com", DEFAULT_RECEIVER, smtpServerSetup);
    sendMessage("anotherAddress@anotherDomain.com", DEFAULT_RECEIVER, smtpServerSetup);
    sendMessage("anotherAddress@anotherDomain.com", DEFAULT_RECEIVER, smtpServerSetup);
    sendMessage("anotherAddress@anotherDomain.com", DEFAULT_RECEIVER, smtpServerSetup);
    MailReceiver mbox = createClient(gm);
    mbox.purge(true);
    mbox.setRegularExpressionCompiler(JAVA_UTIL);
    mbox.setSubjectFilter(".*Junit.*");
    MailReceiver mboxChecker = createClient(gm);
    try {
      log.warn(getName() + " connecting");
      mbox.connect();
      assertEquals(5, mbox.getMessages().size());

      for (MimeMessage msg : mbox.getMessages()) {
        mbox.setMessageRead(msg);
        assertTo(msg, DEFAULT_RECEIVER);
      }
      log.warn(getName() + " disconnecting");
      IOUtils.closeQuietly(mbox);

      mboxChecker.connect();
      assertEquals(0, mboxChecker.getMessages().size());
    }
    finally {
      IOUtils.closeQuietly(mbox);
      IOUtils.closeQuietly(mboxChecker);
      stopServer(gm);
      Thread.currentThread().setName(name);
    }
  }

  @Test
  public void testJavaUtil_FromFilter() throws Exception {
    Assume.assumeTrue(testsEnabled());

    GreenMail gm = startServer(DEFAULT_RECEIVER, DEFAULT_POP3_USER, DEFAULT_POP3_PASSWORD);
    ServerSetup smtpServerSetup = new ServerSetup(gm.getSmtp().getPort(), null, ServerSetup.PROTOCOL_SMTP);
    sendMessage(DEFAULT_SENDER, DEFAULT_RECEIVER, smtpServerSetup);
    sendMessage(DEFAULT_SENDER, "anotherAddress@anotherDomain.com", smtpServerSetup);
    MailReceiver mbox = createClient(gm);
    mbox.setRegularExpressionCompiler(JAVA_UTIL);
    mbox.setFromFilter(DEFAULT_SENDER);

    try {
      mbox.connect();
      assertEquals(1, mbox.getMessages().size());
      for (MimeMessage msg : mbox.getMessages()) {
        mbox.setMessageRead(msg);
        printMessageInfo(msg);
        assertTo(msg, DEFAULT_RECEIVER);
        assertFrom(msg, DEFAULT_SENDER);
      }
    }
    finally {
      IOUtils.closeQuietly(mbox);
      stopServer(gm);
    }
  }

  @Test
  public void testJavaUtil_ToFilter() throws Exception {
    Assume.assumeTrue(testsEnabled());
    GreenMail gm = startServer(DEFAULT_RECEIVER, DEFAULT_POP3_USER, DEFAULT_POP3_PASSWORD);
    ServerSetup smtpServerSetup = new ServerSetup(gm.getSmtp().getPort(), null, ServerSetup.PROTOCOL_SMTP);
    sendMessage(DEFAULT_SENDER, DEFAULT_RECEIVER, smtpServerSetup);
    sendMessage(DEFAULT_SENDER, "anotherAddress@anotherDomain.com", smtpServerSetup);
    MailReceiver mbox = createClient(gm);
    mbox.setRegularExpressionCompiler(JAVA_UTIL);
    mbox.setRecipientFilter(".*" + DEFAULT_RECEIVER + ".*");

    try {
      mbox.connect();
      assertEquals(1, mbox.getMessages().size());
      for (MimeMessage msg : mbox.getMessages()) {
        mbox.setMessageRead(msg);
        printMessageInfo(msg);
        assertTo(msg, DEFAULT_RECEIVER);
        assertFrom(msg, DEFAULT_SENDER);
      }
    }
    finally {
      IOUtils.closeQuietly(mbox);
      stopServer(gm);
    }
  }

  @Test
  public void testJavaUtil_ToFilterNoMatch() throws Exception {
    Assume.assumeTrue(testsEnabled());
    GreenMail gm = startServer(DEFAULT_RECEIVER, DEFAULT_POP3_USER, DEFAULT_POP3_PASSWORD);
    ServerSetup smtpServerSetup = new ServerSetup(gm.getSmtp().getPort(), null, ServerSetup.PROTOCOL_SMTP);
    sendMessage(DEFAULT_SENDER, DEFAULT_RECEIVER, smtpServerSetup);
    sendMessage(DEFAULT_SENDER, "anotherAddress@anotherDomain.com", smtpServerSetup);
    MailReceiver mbox = createClient(gm);
    mbox.setRegularExpressionCompiler(JAVA_UTIL);
    mbox.setRecipientFilter("ABCDEFG");

    try {
      mbox.connect();
      assertEquals(0, mbox.getMessages().size());
    }
    finally {
      IOUtils.closeQuietly(mbox);
      stopServer(gm);
    }
  }

  @Test
  public void testJavaUtil_CustomFilter() throws Exception {
    Assume.assumeTrue(testsEnabled());
    GreenMail gm = startServer(DEFAULT_RECEIVER, DEFAULT_POP3_USER, DEFAULT_POP3_PASSWORD);
    ServerSetup smtpServerSetup = new ServerSetup(gm.getSmtp().getPort(), null, ServerSetup.PROTOCOL_SMTP);
    sendMessage(DEFAULT_SENDER, DEFAULT_RECEIVER, smtpServerSetup);
    sendMessage(DEFAULT_SENDER, "anotherAddress@anotherDomain.com", smtpServerSetup);
    MailReceiver mbox = createClient(gm);
    mbox.setRegularExpressionCompiler(JAVA_UTIL);
    mbox.addCustomFilter("From", ".*" + DEFAULT_SENDER + ".*");
    try {
      mbox.connect();
      assertEquals(1, mbox.getMessages().size());
      for (MimeMessage msg : mbox.getMessages()) {
        mbox.setMessageRead(msg);
        printMessageInfo(msg);
        assertTo(msg, DEFAULT_RECEIVER);
        assertFrom(msg, DEFAULT_SENDER);
      }
    }
    finally {
      IOUtils.closeQuietly(mbox);
      stopServer(gm);
    }
  }

  @Test
  public void testJavaUtil_SubjectFilter() throws Exception {
    Assume.assumeTrue(testsEnabled());

    GreenMail gm = startServer(DEFAULT_RECEIVER, DEFAULT_POP3_USER, DEFAULT_POP3_PASSWORD);
    ServerSetup smtpServerSetup = new ServerSetup(gm.getSmtp().getPort(), null, ServerSetup.PROTOCOL_SMTP);
    sendMessage(DEFAULT_SENDER, DEFAULT_RECEIVER, smtpServerSetup);
    sendMessage(DEFAULT_SENDER, "anotherAddress@anotherDomain.com", smtpServerSetup);
    MailReceiver mbox = createClient(gm);
    mbox.setRegularExpressionCompiler(JAVA_UTIL);
    mbox.setSubjectFilter(".*Junit.*");
    try {
      mbox.connect();
      assertEquals(1, mbox.getMessages().size());
      for (MimeMessage msg : mbox.getMessages()) {
        mbox.setMessageRead(msg);
        printMessageInfo(msg);
        assertTo(msg, DEFAULT_RECEIVER);
        assertFrom(msg, DEFAULT_SENDER);
      }
    }
    finally {
      IOUtils.closeQuietly(mbox);
      stopServer(gm);
    }
  }

  @Test
  public void testJavaUtil_FromSubjectFilter_NullSubject() throws Exception {
    Assume.assumeTrue(testsEnabled());

    GreenMail gm = startServer(DEFAULT_RECEIVER, DEFAULT_POP3_USER, DEFAULT_POP3_PASSWORD);
    ServerSetup smtpServerSetup = new ServerSetup(gm.getSmtp().getPort(), null, ServerSetup.PROTOCOL_SMTP);
    sendMessage(DEFAULT_SENDER, DEFAULT_RECEIVER, null, smtpServerSetup);
    MailReceiver mbox = createClient(gm);
    mbox.setRegularExpressionCompiler(JAVA_UTIL);
    mbox.setFromFilter(DEFAULT_SENDER);
    mbox.setSubjectFilter(".*Junit*");

    MailReceiver checker = createClient(gm);
    checker.setFromFilter(DEFAULT_SENDER);
    try {
      mbox.connect();
      assertEquals(0, mbox.getMessages().size());
      checker.connect();
      assertEquals(1, checker.getMessages().size());
    }
    finally {
      IOUtils.closeQuietly(mbox);
      IOUtils.closeQuietly(checker);
      stopServer(gm);
    }
  }

  @Test
  public void testJavaUtil_FromSubjectFilter_WithDelete() throws Exception {
    Assume.assumeTrue(testsEnabled());

    GreenMail gm = startServer(DEFAULT_RECEIVER, DEFAULT_POP3_USER, DEFAULT_POP3_PASSWORD);
    ServerSetup smtpServerSetup = new ServerSetup(gm.getSmtp().getPort(), null, ServerSetup.PROTOCOL_SMTP);
    sendMessage(DEFAULT_SENDER, DEFAULT_RECEIVER, smtpServerSetup);
    sendMessage(DEFAULT_SENDER, "anotherAddress@anotherDomain.com", smtpServerSetup);
    MailReceiver mbox = createClient(gm);
    mbox.setRegularExpressionCompiler(JAVA_UTIL);
    mbox.setFromFilter(DEFAULT_SENDER);
    mbox.setSubjectFilter(".*Junit.*");
    mbox.purge(true);
    try {
      mbox.connect();
      assertEquals(1, mbox.getMessages().size());
      for (MimeMessage msg : mbox.getMessages()) {
        mbox.setMessageRead(msg);
        printMessageInfo(msg);
        assertTo(msg, DEFAULT_RECEIVER);
        assertFrom(msg, DEFAULT_SENDER);
      }
      IOUtils.closeQuietly(mbox);
      mbox.connect();
      assertEquals(0, mbox.getMessages().size());
      IOUtils.closeQuietly(mbox);
    }
    finally {
      IOUtils.closeQuietly(mbox);
      stopServer(gm);
    }
  }

  protected void printMessageInfo(MimeMessage msg) throws Exception {
    MessageParser mp = new MessageParser(msg);
    logger.debug("Got Message :- " + msg.getSubject());
    logger.trace("With ID: " + mp.getMessageId());
    Enumeration<Header> headers = msg.getAllHeaders();
    while (headers.hasMoreElements()) {
      Header header = headers.nextElement();
      logger.trace("HeaderLine " + header.getName() + ": " + header.getValue());
    }
    if (mp.hasAttachments()) {
      while (mp.hasMoreAttachments()) {
        Attachment a = mp.nextAttachment();
        logger.trace("Contains Attachment : " + a);
      }
    }
  }

  protected static final URLName createURLName(String urlString, String uname, String pw) throws PasswordException {
    URLName url = new URLName(urlString);
    String password = url.getPassword();
    String username = url.getUsername();
    if (username == null && !isEmpty(uname)) {
      username = uname;
    }
    if (url.getPassword() == null && pw != null) {
      password = Password.decode(pw);
    }
    return new URLName(url.getProtocol(), url.getHost(), url.getPort(), url.getFile(), username, password);
  }
}
