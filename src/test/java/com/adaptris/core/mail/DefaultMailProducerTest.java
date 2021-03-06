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

package com.adaptris.core.mail;

import static com.adaptris.mail.JunitMailHelper.DEFAULT_RECEIVER;
import static com.adaptris.mail.JunitMailHelper.DEFAULT_SENDER;
import static com.adaptris.mail.JunitMailHelper.testsEnabled;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.mail.Header;
import javax.mail.internet.MimeMessage;
import org.junit.Assume;
import org.junit.Test;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;
import com.adaptris.core.ConfiguredProduceDestination;
import com.adaptris.core.NullConnection;
import com.adaptris.core.ServiceCase;
import com.adaptris.core.StandaloneProducer;
import com.adaptris.core.metadata.RegexMetadataFilter;
import com.adaptris.mail.JunitMailHelper;
import com.adaptris.mail.MessageParser;
import com.adaptris.util.KeyValuePair;
import com.icegreen.greenmail.smtp.SmtpServer;
import com.icegreen.greenmail.util.GreenMail;

@SuppressWarnings("deprecation")
public class DefaultMailProducerTest extends MailProducerExample {


  @Override
  protected Object retrieveObjectForSampleConfig() {
    return null;
  }

  @Test
  public void testSetContentType() {
    DefaultSmtpProducer producer = new DefaultSmtpProducer();
    assertNull(producer.getContentType());
    assertEquals("text/plain", producer.contentType());
    producer.setContentType("x");
    assertEquals("x", producer.getContentType());
    assertEquals("x", producer.contentType());
  }

  @Test
  public void testSetContentEncoding() {
    DefaultSmtpProducer producer = new DefaultSmtpProducer();
    assertNull(producer.getContentEncoding());
    assertEquals("base64", producer.contentEncoding());
    producer.setContentEncoding("x");
    assertEquals("x", producer.getContentEncoding());
    assertEquals("x", producer.contentEncoding());
  }

  @Test
  public void testSetAttachmentContentType() {
    DefaultSmtpProducer producer = new DefaultSmtpProducer();
    assertNull(producer.getAttachmentContentType());
    assertEquals("application/octet-stream", producer.attachmentContentType());
    producer.setAttachmentContentType("x");
    assertEquals("x", producer.getAttachmentContentType());
    assertEquals("x", producer.attachmentContentType());
  }

  @Test
  public void testSetAttachmentContentEncoding() {
    DefaultSmtpProducer producer = new DefaultSmtpProducer();
    assertNull(producer.getAttachmentContentEncoding());
    assertEquals("base64", producer.attachmentContentEncoding());
    producer.setAttachmentContentEncoding("x");
    assertEquals("x", producer.getAttachmentContentEncoding());
    assertEquals("x", producer.attachmentContentEncoding());
  }

  @Test
  public void testProduce() throws Exception {
    Assume.assumeTrue(testsEnabled());
    GreenMail gm = mailServer();
    AdaptrisMessage msg =
        AdaptrisMessageFactory.getDefaultInstance().newMessage(JunitMailHelper.DEFAULT_PAYLOAD);
    ServiceCase.execute(createProducerForTests(gm), msg);
    gm.waitForIncomingEmail(1);
    MimeMessage[] msgs = gm.getReceivedMessages();
    assertEquals(1, msgs.length);
    JunitMailHelper.assertFrom(msgs[0], DEFAULT_SENDER);
    JunitMailHelper.assertTo(msgs[0], DEFAULT_RECEIVER);
  }

  @Test
  public void testProduceCC() throws Exception {
    Assume.assumeTrue(testsEnabled());
    GreenMail gm = mailServer();
    AdaptrisMessage msg =
        AdaptrisMessageFactory.getDefaultInstance().newMessage(JunitMailHelper.DEFAULT_PAYLOAD);
    StandaloneProducer producer = createProducerForTests(gm);
    MailProducer mailer = (MailProducer) producer.getProducer();
    mailer.setCcList("CarbonCopy@CarbonCopy.com");
    ServiceCase.execute(producer, msg);
    gm.waitForIncomingEmail(1);
    MimeMessage[] msgs = gm.getReceivedMessages();
    assertEquals(2, msgs.length);
    for (MimeMessage mime : msgs) {
      JunitMailHelper.assertFrom(mime, DEFAULT_SENDER);
      JunitMailHelper.assertTo(mime, DEFAULT_RECEIVER);
      JunitMailHelper.assertCC(mime, "CarbonCopy@CarbonCopy.com");
    }
  }

  @Test
  public void testProduceMetadataSubjectCC() throws Exception {
    Assume.assumeTrue(testsEnabled());
    GreenMail gm = mailServer();
    AdaptrisMessage msg =
        AdaptrisMessageFactory.getDefaultInstance().newMessage(JunitMailHelper.DEFAULT_PAYLOAD);
    msg.addMetadata(EmailConstants.EMAIL_CC_LIST, "CarbonCopy@CarbonCopy.com"); // these will force
                                                                                // the use of (the
    msg.addMetadata(EmailConstants.EMAIL_SUBJECT, "Some very important subject"); // now deprecated)
                                                                                  // metadata keys
    StandaloneProducer producer = createProducerForTests(gm);
    ServiceCase.execute(producer, msg);
    gm.waitForIncomingEmail(1);
    MimeMessage[] msgs = gm.getReceivedMessages();
    assertEquals(2, msgs.length);
    for (MimeMessage mime : msgs) {
      JunitMailHelper.assertFrom(mime, DEFAULT_SENDER);
      JunitMailHelper.assertTo(mime, DEFAULT_RECEIVER);
      JunitMailHelper.assertCC(mime, "CarbonCopy@CarbonCopy.com");
    }

  }

  @Test
  public void testProduceNoAddresseeButResolveCC() throws Exception {
    GreenMail gm = mailServer();
    AdaptrisMessage msg =
        AdaptrisMessageFactory.getDefaultInstance().newMessage(JunitMailHelper.DEFAULT_PAYLOAD);
    msg.addMetadata("some-cc-address", "CarbonCopy@CarbonCopy.com");
    StandaloneProducer producer = createProducerForTests(gm);
    MailProducer mailer = (MailProducer) producer.getProducer();
    mailer.setDestination(null);
    mailer.setCcList("%message{some-cc-address}");
    ServiceCase.execute(producer, msg);
    gm.waitForIncomingEmail(1);
    MimeMessage[] msgs = gm.getReceivedMessages();
    assertEquals(1, msgs.length);
    for (MimeMessage mime : msgs) {
      JunitMailHelper.assertFrom(mime, DEFAULT_SENDER);
      JunitMailHelper.assertRecipientNull(mime);
      JunitMailHelper.assertCC(mime, "CarbonCopy@CarbonCopy.com");
    }
  }

  @Test
  public void testProduceBCC() throws Exception {
    GreenMail gm = mailServer();
    AdaptrisMessage msg =
        AdaptrisMessageFactory.getDefaultInstance().newMessage(JunitMailHelper.DEFAULT_PAYLOAD);
    StandaloneProducer producer = createProducerForTests(gm);
    MailProducer mailer = (MailProducer) producer.getProducer();
    mailer.setBccList("BlindCarbonCopy@BlindCarbonCopy.com");
    ServiceCase.execute(producer, msg);
    gm.waitForIncomingEmail(1);
    MimeMessage[] msgs = gm.getReceivedMessages();
    assertEquals(2, msgs.length);
    for (MimeMessage mime : msgs) {
      JunitMailHelper.assertFrom(mime, DEFAULT_SENDER);
      JunitMailHelper.assertTo(mime, DEFAULT_RECEIVER);
      // We never *see* the BCC so we can't check it.
    }
  }

  @Test
  public void testProduceNoAddresseeButResolveBCC() throws Exception {
    Assume.assumeTrue(testsEnabled());
    GreenMail gm = mailServer();
    AdaptrisMessage msg =
        AdaptrisMessageFactory.getDefaultInstance().newMessage(JunitMailHelper.DEFAULT_PAYLOAD);
    msg.addMetadata("a-bcc-address", "BlindCarbonCopy@BlindCarbonCopy.com");
    StandaloneProducer producer = createProducerForTests(gm);
    MailProducer mailer = (MailProducer) producer.getProducer();
    mailer.setDestination(null);
    mailer.setBccList("%message{a-bcc-address}");
    ServiceCase.execute(producer, msg);
    gm.waitForIncomingEmail(1);
    MimeMessage[] msgs = gm.getReceivedMessages();
    assertEquals(1, msgs.length);
    for (MimeMessage mime : msgs) {
      JunitMailHelper.assertFrom(mime, DEFAULT_SENDER);
      JunitMailHelper.assertRecipientNull(mime);
      // We never *see* the BCC so we can't check it.
    }
  }

  @Test
  public void testProduceWithHeaders() throws Exception {
    Assume.assumeTrue(testsEnabled());
    GreenMail gm = mailServer();
    AdaptrisMessage msg =
        AdaptrisMessageFactory.getDefaultInstance().newMessage(JunitMailHelper.DEFAULT_PAYLOAD);
    msg.addMetadata("X-Email-Producer", "ABCDEFG");
    StandaloneProducer producer = createProducerForTests(gm);
    MailProducer mailer = (MailProducer) producer.getProducer();
    RegexMetadataFilter filter = new RegexMetadataFilter();
    filter.addIncludePattern("X-Email.*");
    mailer.setMetadataFilter(filter);
    ServiceCase.execute(producer, msg);

    gm.waitForIncomingEmail(1);
    MimeMessage[] msgs = gm.getReceivedMessages();
    assertEquals(1, msgs.length);
    MimeMessage mailmsg = msgs[0];
    JunitMailHelper.assertFrom(mailmsg, DEFAULT_SENDER);
    JunitMailHelper.assertTo(mailmsg, DEFAULT_RECEIVER);
    Enumeration<Header> headers = mailmsg.getAllHeaders();
    boolean matched = false;
    while (headers.hasMoreElements()) {
      Header header = headers.nextElement();
      if (header.getName().equals("X-Email-Producer")) {
        if (header.getValue().equals("ABCDEFG")) {
          matched = true;
          break;
        }
      }
    }
    if (!matched) {
      fail("Additional Metadata Headers were not produced");
    }
  }

  @Test
  public void testProduceAsAttachment() throws Exception {
    Assume.assumeTrue(testsEnabled());
    GreenMail gm = mailServer();
    AdaptrisMessage msg =
        AdaptrisMessageFactory.getDefaultInstance().newMessage(JunitMailHelper.DEFAULT_PAYLOAD);
    StandaloneProducer producer = createProducerForTests(gm);
    DefaultSmtpProducer mailer = (DefaultSmtpProducer) producer.getProducer();
    mailer.setIsAttachment(true);
    mailer.setAttachmentContentType("text/plain");
    ServiceCase.execute(producer, msg);

    gm.waitForIncomingEmail(1);
    MimeMessage[] msgs = gm.getReceivedMessages();
    assertEquals(1, msgs.length);
    MimeMessage mailmsg = msgs[0];
    JunitMailHelper.assertFrom(mailmsg, DEFAULT_SENDER);
    JunitMailHelper.assertTo(mailmsg, DEFAULT_RECEIVER);
    MessageParser mp = new MessageParser(mailmsg);
    assertTrue(mp.hasAttachments());
  }

  @Test
  public void testProduceAsAttachmentWithFilename() throws Exception {
    Assume.assumeTrue(testsEnabled());
    GreenMail gm = mailServer();
    AdaptrisMessage msg =
        AdaptrisMessageFactory.getDefaultInstance().newMessage(JunitMailHelper.DEFAULT_PAYLOAD);
    StandaloneProducer producer = createProducerForTests(gm);
    DefaultSmtpProducer mailer = (DefaultSmtpProducer) producer.getProducer();
    mailer.setIsAttachment(true);
    mailer.setAttachmentContentType("text/xml");
    msg.addMetadata(EmailConstants.EMAIL_ATTACH_FILENAME, "filename.txt");
    ServiceCase.execute(producer, msg);

    gm.waitForIncomingEmail(1);
    MimeMessage[] msgs = gm.getReceivedMessages();
    assertEquals(1, msgs.length);
    MimeMessage mailmsg = msgs[0];
    JunitMailHelper.assertFrom(mailmsg, DEFAULT_SENDER);
    JunitMailHelper.assertTo(mailmsg, DEFAULT_RECEIVER);
    MessageParser mp = new MessageParser(mailmsg);
    assertTrue(mp.hasAttachments());
    assertEquals("filename.txt", mp.nextAttachment().getFilename());
  }

  @Test
  public void testProduceAsAttachmentWithMetadataAttachmentContentType() throws Exception {
    Assume.assumeTrue(testsEnabled());
    GreenMail gm = mailServer();
    AdaptrisMessage msg =
        AdaptrisMessageFactory.getDefaultInstance().newMessage(JunitMailHelper.DEFAULT_PAYLOAD);
    StandaloneProducer producer = createProducerForTests(gm);
    DefaultSmtpProducer mailer = (DefaultSmtpProducer) producer.getProducer();
    mailer.setIsAttachment(true);
    mailer.setAttachmentContentType("text/xml");
    msg.addMetadata(EmailConstants.EMAIL_ATTACH_CONTENT_TYPE, "text/plain");
    ServiceCase.execute(producer, msg);

    gm.waitForIncomingEmail(1);
    MimeMessage[] msgs = gm.getReceivedMessages();
    assertEquals(1, msgs.length);
    MimeMessage mailmsg = msgs[0];
    JunitMailHelper.assertFrom(mailmsg, DEFAULT_SENDER);
    JunitMailHelper.assertTo(mailmsg, DEFAULT_RECEIVER);
    MessageParser mp = new MessageParser(mailmsg);
    assertTrue(mp.hasAttachments());
    assertEquals("text/plain", mp.nextAttachment().getContentType());

  }

  @Test
  public void testProduceAsAttachmentWithTemplate() throws Exception {
    Assume.assumeTrue(testsEnabled());
    GreenMail gm = mailServer();
    AdaptrisMessage msg =
        AdaptrisMessageFactory.getDefaultInstance().newMessage(JunitMailHelper.DEFAULT_PAYLOAD);
    StandaloneProducer producer = createProducerForTests(gm);
    DefaultSmtpProducer mailer = (DefaultSmtpProducer) producer.getProducer();
    mailer.setIsAttachment(true);
    mailer.setAttachmentContentType("text/plain");
    msg.addMetadata(EmailConstants.EMAIL_TEMPLATE_BODY, "This is the body");
    ServiceCase.execute(producer, msg);

    gm.waitForIncomingEmail(1);
    MimeMessage[] msgs = gm.getReceivedMessages();
    assertEquals(1, msgs.length);
    MimeMessage mailmsg = msgs[0];
    JunitMailHelper.assertFrom(mailmsg, DEFAULT_SENDER);
    JunitMailHelper.assertTo(mailmsg, DEFAULT_RECEIVER);
    MessageParser mp = new MessageParser(mailmsg);
    assertTrue(mp.hasAttachments());
    assertEquals("This is the body", new String(mp.getMessage()));
  }

  @Test
  public void testProduceAsAttachmentWithCharEncodingForTemplate() throws Exception {
    Assume.assumeTrue(testsEnabled());
    GreenMail gm = mailServer();
    AdaptrisMessage msg = AdaptrisMessageFactory.getDefaultInstance()
        .newMessage(JunitMailHelper.DEFAULT_PAYLOAD, "UTF-8");
    StandaloneProducer producer = createProducerForTests(gm);
    DefaultSmtpProducer mailer = (DefaultSmtpProducer) producer.getProducer();
    mailer.setIsAttachment(true);
    mailer.setAttachmentContentType("text/plain");
    msg.addMetadata(EmailConstants.EMAIL_TEMPLATE_BODY, "This is the body");
    ServiceCase.execute(producer, msg);

    gm.waitForIncomingEmail(1);
    MimeMessage[] msgs = gm.getReceivedMessages();
    assertEquals(1, msgs.length);
    MimeMessage mailmsg = msgs[0];
    JunitMailHelper.assertFrom(mailmsg, DEFAULT_SENDER);
    JunitMailHelper.assertTo(mailmsg, DEFAULT_RECEIVER);
    MessageParser mp = new MessageParser(mailmsg);
    assertTrue(mp.hasAttachments());
    assertEquals("This is the body", new String(mp.getMessage()));


  }

  /**
   * @see com.adaptris.core.ExampleConfigCase#retrieveObjectForSampleConfig()
   */
  @Override
  protected List<StandaloneProducer> retrieveObjectsForSampleConfig() {
    List<StandaloneProducer> result = new ArrayList<>();
    DefaultSmtpProducer smtp = new DefaultSmtpProducer();
    smtp.setTo("user@domain");
    smtp.getSessionProperties()
        .addKeyValuePair(new KeyValuePair("mail.smtp.starttls.enable", "true"));
    smtp.setSubject("Configured subject");
    smtp.setSmtpUrl("smtp://localhost:25");
    smtp.setCcList("user@domain, user@domain");
    RegexMetadataFilter filter = new RegexMetadataFilter();
    filter.addIncludePattern("X-MyHeaders.*");
    smtp.setMetadataFilter(filter);
    result.add(new StandaloneProducer(smtp));

    DefaultSmtpProducer smtps = new DefaultSmtpProducer();
    smtps.setTo("user@domain");
    smtps.getSessionProperties()
        .addKeyValuePair(new KeyValuePair("mail.smtp.starttls.enable", "true"));
    smtps.setSubject("Configured subject");
    smtps.setSmtpUrl("smtps://username%40gmail.com:mypassword;@smtp.gmail.com:465");
    smtps.setCcList("user@domain, user@domain");
    filter = new RegexMetadataFilter();
    filter.addIncludePattern("X-MyHeaders.*");
    smtps.setMetadataFilter(filter);
    result.add(new StandaloneProducer(smtps));

    return result;
  }

  @Override
  protected String createBaseFileName(Object object) {
    String basename = super.createBaseFileName(object);
    StandaloneProducer c = (StandaloneProducer) object;
    String s = ((DefaultSmtpProducer) c.getProducer()).getSmtpUrl();
    int pos = s.indexOf(":");
    if (pos > 0) {
      basename = basename + "-" + s.substring(0, pos).toUpperCase();
    }
    return basename;
  }

  protected StandaloneProducer createProducerForTests(GreenMail gm) {
    DefaultSmtpProducer smtp = new DefaultSmtpProducer();
    SmtpServer server = gm.getSmtp();
    String smtpUrl = server.getProtocol() + "://localhost:" + server.getPort();
    smtp.setSmtpUrl(smtpUrl);
    smtp.setSubject("Junit Test for com.adaptris.core.mail");
    smtp.setFrom(JunitMailHelper.DEFAULT_SENDER);
    smtp.setContentType("plain/text");
    smtp.setDestination(new ConfiguredProduceDestination(JunitMailHelper.DEFAULT_RECEIVER));
    return new StandaloneProducer(new NullConnection(), smtp);
  }

}
