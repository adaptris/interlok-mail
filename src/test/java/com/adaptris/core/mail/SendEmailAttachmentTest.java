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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.ArrayList;
import java.util.List;
import javax.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;
import com.adaptris.core.NullConnection;
import com.adaptris.core.StandaloneProducer;
import com.adaptris.core.common.ConstantDataInputParameter;
import com.adaptris.core.metadata.RegexMetadataFilter;
import com.adaptris.interlok.junit.scaffolding.services.ExampleServiceCase;
import com.adaptris.mail.JunitMailHelper;
import com.adaptris.mail.MessageParser;
import com.adaptris.util.KeyValuePair;
import com.icegreen.greenmail.smtp.SmtpServer;
import com.icegreen.greenmail.util.GreenMail;

public class SendEmailAttachmentTest extends MailProducerExample {

  @Override
  protected Object retrieveObjectForSampleConfig() {
    return null;
  }



  @Test
  public void testProduceAsAttachment() throws Exception {
    assumeTrue(testsEnabled());
    GreenMail gm = mailServer();
    AdaptrisMessage msg =
        AdaptrisMessageFactory.getDefaultInstance().newMessage(JunitMailHelper.DEFAULT_PAYLOAD);
    StandaloneProducer producer = createProducerForTests(gm);
    SendEmailAttachment mailer = (SendEmailAttachment) producer.getProducer();
    mailer.setContentType(EmailConstants.TEXT_PLAIN);
    ExampleServiceCase.execute(producer, msg);

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
    assumeTrue(testsEnabled());
    GreenMail gm = mailServer();
    AdaptrisMessage msg =
        AdaptrisMessageFactory.getDefaultInstance().newMessage(JunitMailHelper.DEFAULT_PAYLOAD);
    StandaloneProducer producer = createProducerForTests(gm);
    SendEmailAttachment mailer = (SendEmailAttachment) producer.getProducer();
    mailer.setContentType("text/xml");
    mailer.setFilename("%message{filename}");
    msg.addMetadata("filename", "filename.txt");
    ExampleServiceCase.execute(producer, msg);

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
    assumeTrue(testsEnabled());
    GreenMail gm = mailServer();
    AdaptrisMessage msg =
        AdaptrisMessageFactory.getDefaultInstance().newMessage(JunitMailHelper.DEFAULT_PAYLOAD);
    StandaloneProducer producer = createProducerForTests(gm);
    SendEmailAttachment mailer = (SendEmailAttachment) producer.getProducer();
    mailer.setAttachmentContentType("%message{contentType}");
    msg.addMetadata("contentType", EmailConstants.TEXT_PLAIN);
    ExampleServiceCase.execute(producer, msg);

    gm.waitForIncomingEmail(1);
    MimeMessage[] msgs = gm.getReceivedMessages();
    assertEquals(1, msgs.length);
    MimeMessage mailmsg = msgs[0];
    JunitMailHelper.assertFrom(mailmsg, DEFAULT_SENDER);
    JunitMailHelper.assertTo(mailmsg, DEFAULT_RECEIVER);
    MessageParser mp = new MessageParser(mailmsg);
    assertTrue(mp.hasAttachments());
    assertEquals(EmailConstants.TEXT_PLAIN, mp.nextAttachment().getContentType());
  }

  @Test
  public void testProduceAsAttachmentWithTemplate() throws Exception {
    assumeTrue(testsEnabled());
    GreenMail gm = mailServer();
    AdaptrisMessage msg =
        AdaptrisMessageFactory.getDefaultInstance().newMessage(JunitMailHelper.DEFAULT_PAYLOAD);
    StandaloneProducer producer = createProducerForTests(gm);
    SendEmailAttachment mailer = (SendEmailAttachment) producer.getProducer();
    mailer.setBody(new ConstantDataInputParameter("This is the body"));
    ExampleServiceCase.execute(producer, msg);

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
    SendEmailAttachment smtp = new SendEmailAttachment();
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

    SendEmailAttachment smtps = new SendEmailAttachment();
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
    String s = ((SendEmailAttachment) c.getProducer()).getSmtpUrl();
    int pos = s.indexOf(":");
    if (pos > 0) {
      basename = basename + "-" + s.substring(0, pos).toUpperCase();
    }
    return basename;
  }

  protected StandaloneProducer createProducerForTests(GreenMail gm) {
    SendEmailAttachment smtp = new SendEmailAttachment();
    SmtpServer server = gm.getSmtp();
    String smtpUrl = server.getProtocol() + "://localhost:" + server.getPort();
    smtp.setSmtpUrl(smtpUrl);
    smtp.setSubject("Junit Test for com.adaptris.core.mail");
    smtp.setFrom(JunitMailHelper.DEFAULT_SENDER);
    smtp.setContentType("plain/text");
    smtp.setTo(JunitMailHelper.DEFAULT_RECEIVER);
    return new StandaloneProducer(new NullConnection(), smtp);
  }

}
