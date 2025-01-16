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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import jakarta.mail.Header;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;
import com.adaptris.core.NullConnection;
import com.adaptris.core.StandaloneProducer;
import com.adaptris.core.UnresolvedMetadataException;
import com.adaptris.core.metadata.RegexMetadataFilter;
import com.adaptris.interlok.junit.scaffolding.services.ExampleServiceCase;
import com.adaptris.mail.JunitMailHelper;
import com.adaptris.util.KeyValuePair;
import com.icegreen.greenmail.smtp.SmtpServer;
import com.icegreen.greenmail.util.GreenMail;

public class SendEmailTest extends MailProducerExample {

  @Override
  protected Object retrieveObjectForSampleConfig() {
    return null;
  }


  @Test
  public void testProduce() throws Exception {
    assumeTrue(testsEnabled());

    GreenMail gm = mailServer();
    AdaptrisMessage msg =
        AdaptrisMessageFactory.getDefaultInstance().newMessage(JunitMailHelper.DEFAULT_PAYLOAD);
    ExampleServiceCase.execute(createProducerForTests(gm), msg);
    gm.waitForIncomingEmail(1);
    MimeMessage[] msgs = gm.getReceivedMessages();
    assertEquals(1, msgs.length);
    JunitMailHelper.assertFrom(msgs[0], DEFAULT_SENDER);
    JunitMailHelper.assertTo(msgs[0], DEFAULT_RECEIVER);
  }
  
  @Test
  public void testProduceFromExpression() throws Exception {
    assumeTrue(testsEnabled());
    GreenMail gm = mailServer();
    AdaptrisMessage msg =
    AdaptrisMessageFactory.getDefaultInstance().newMessage(JunitMailHelper.DEFAULT_PAYLOAD);
    StandaloneProducer producer = createProducerForTests(gm);
    MailProducer mailer = (MailProducer) producer.getProducer();
    msg.addMetadata("fromKey", "test@test.com");
    mailer.setFrom(msg.resolve("%message{fromKey}"));
    assertEquals(mailer.getFrom(), "test@test.com");
  }
  
  @Test
  public void testProduceFromExpressionMetadataKeyNotFound() throws Exception {
    assumeTrue(testsEnabled());
    GreenMail gm = mailServer();
    AdaptrisMessage msg =
    AdaptrisMessageFactory.getDefaultInstance().newMessage(JunitMailHelper.DEFAULT_PAYLOAD);
    StandaloneProducer producer = createProducerForTests(gm);
    MailProducer mailer = (MailProducer) producer.getProducer();
    msg.addMetadata("fromKey", "test@test.com");
    assertThrows(UnresolvedMetadataException.class, ()->{
      mailer.setFrom(msg.resolve("%message{notFound}"));
    }, "Could not resolve metadata key");
  }

  @Test
  public void testProduceCC() throws Exception {
    assumeTrue(testsEnabled());
    GreenMail gm = mailServer();
    AdaptrisMessage msg =
        AdaptrisMessageFactory.getDefaultInstance().newMessage(JunitMailHelper.DEFAULT_PAYLOAD);
    StandaloneProducer producer = createProducerForTests(gm);
    MailProducer mailer = (MailProducer) producer.getProducer();
    mailer.setCcList("CarbonCopy@CarbonCopy.com");
    ExampleServiceCase.execute(producer, msg);
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
    assumeTrue(testsEnabled());
    GreenMail gm = mailServer();
    AdaptrisMessage msg =
        AdaptrisMessageFactory.getDefaultInstance().newMessage(JunitMailHelper.DEFAULT_PAYLOAD);
    msg.addMetadata("some-cc-address", "CarbonCopy@CarbonCopy.com");
    StandaloneProducer producer = createProducerForTests(gm);
    MailProducer mailer = (MailProducer) producer.getProducer();
    mailer.setTo(null);
    mailer.setCcList("%message{some-cc-address}");
    ExampleServiceCase.execute(producer, msg);
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
    assumeTrue(testsEnabled());
    GreenMail gm = mailServer();
    AdaptrisMessage msg =
        AdaptrisMessageFactory.getDefaultInstance().newMessage(JunitMailHelper.DEFAULT_PAYLOAD);
    StandaloneProducer producer = createProducerForTests(gm);
    MailProducer mailer = (MailProducer) producer.getProducer();
    mailer.setBccList("BlindCarbonCopy@BlindCarbonCopy.com");
    ExampleServiceCase.execute(producer, msg);
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
    assumeTrue(testsEnabled());
    GreenMail gm = mailServer();
    AdaptrisMessage msg =
        AdaptrisMessageFactory.getDefaultInstance().newMessage(JunitMailHelper.DEFAULT_PAYLOAD);
    msg.addMetadata("a-bcc-address", "BlindCarbonCopy@BlindCarbonCopy.com");
    StandaloneProducer producer = createProducerForTests(gm);
    MailProducer mailer = (MailProducer) producer.getProducer();
    mailer.setTo(null);
    mailer.setBccList("%message{a-bcc-address}");
    ExampleServiceCase.execute(producer, msg);
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
    assumeTrue(testsEnabled());
    GreenMail gm = mailServer();
    AdaptrisMessage msg =
        AdaptrisMessageFactory.getDefaultInstance().newMessage(JunitMailHelper.DEFAULT_PAYLOAD);
    msg.addMetadata("X-Email-Producer", "ABCDEFG");
    StandaloneProducer producer = createProducerForTests(gm);
    MailProducer mailer = (MailProducer) producer.getProducer();
    mailer.setFrom(null);
    mailer.getSessionProperties().addKeyValuePair(new KeyValuePair("a", "b"));
    RegexMetadataFilter filter = new RegexMetadataFilter();
    filter.addIncludePattern("X-Email.*");
    mailer.setMetadataFilter(filter);
    ExampleServiceCase.execute(producer, msg);

    gm.waitForIncomingEmail(1);
    MimeMessage[] msgs = gm.getReceivedMessages();
    assertEquals(1, msgs.length);
    MimeMessage mailmsg = msgs[0];
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

  /**
   * @see com.adaptris.core.ExampleConfigCase#retrieveObjectForSampleConfig()
   */
  @Override
  protected List<StandaloneProducer> retrieveObjectsForSampleConfig() {
    List<StandaloneProducer> result = new ArrayList<>();
    SendEmail smtp = new SendEmail();
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

    SendEmail smtps = new SendEmail();
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
    String s = ((SendEmail) c.getProducer()).getSmtpUrl();
    int pos = s.indexOf(":");
    if (pos > 0) {
      basename = basename + "-" + s.substring(0, pos).toUpperCase();
    }
    return basename;
  }

  protected StandaloneProducer createProducerForTests(GreenMail gm) {
    SendEmail smtp = new SendEmail();
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
