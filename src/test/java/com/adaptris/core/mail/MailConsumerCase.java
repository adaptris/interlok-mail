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

import com.adaptris.core.Adapter;
import com.adaptris.core.AdaptrisMessageProducer;
import com.adaptris.core.Channel;
import com.adaptris.core.ChannelList;
import com.adaptris.core.FixedIntervalPoller;
import com.adaptris.core.NullConnection;
import com.adaptris.core.Poller;
import com.adaptris.core.QuartzCronPoller;
import com.adaptris.core.ServiceList;
import com.adaptris.core.StandaloneConsumer;
import com.adaptris.core.StandardWorkflow;
import com.adaptris.core.Workflow;
import com.adaptris.core.WorkflowList;
import com.adaptris.core.stubs.MockMessageProducer;
import com.adaptris.mail.JavamailReceiverFactory;
import com.adaptris.mail.JunitMailHelper;
import com.adaptris.mail.Mail;
import com.adaptris.mail.MailReceiverFactory;
import com.adaptris.security.password.Password;
import com.adaptris.util.GuidGenerator;
import com.adaptris.util.KeyValuePair;
import com.adaptris.util.TimeInterval;
import com.icegreen.greenmail.pop3.Pop3Server;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.InternetHeaders;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static com.adaptris.mail.JunitMailHelper.DEFAULT_RECEIVER;
import static com.adaptris.mail.JunitMailHelper.startServer;
import static com.adaptris.mail.JunitMailHelper.testsEnabled;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public abstract class MailConsumerCase extends MailConsumerExample {

  static final String REGEX_STYLE = "Regex";
  static final String[] TEXT_PAYLOADS = {"The Quick Brown Fox Jumps Over the Lazy Dog",
      "The Quicker Browner Hound Leaps At The Fox",
  "R2D2 and C3PO go into a bar"};
  static final String DEFAULT_SUBJECT = "Junit Test for com.adaptris.core.mail";
  static final String DEFAULT_POP3_USER = "junit";
  static final String DEFAULT_POP3_PASSWORD = "junit";
  static final String DEFAULT_ENCODED_POP3_PASSWORD;

  static {
    try {
      DEFAULT_ENCODED_POP3_PASSWORD = Password.encode(DEFAULT_POP3_PASSWORD, Password.PORTABLE_PASSWORD);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static GreenMail gm;

  @BeforeAll
  public static void setupGreenmail() throws Exception {
    gm = startServer(DEFAULT_RECEIVER, DEFAULT_POP3_USER, DEFAULT_POP3_PASSWORD);
  }

  @AfterAll
  public static void tearDownGreenmail() throws Exception {
    JunitMailHelper.stopServer(gm);
  }

  @BeforeEach
  public void before() throws Exception {
    assumeTrue(testsEnabled());
    gm.purgeEmailFromAllMailboxes();
  }

  protected static GreenMail mailServer() {
    return gm;
  }

  @Test
  public void testAttemptConnectOnInit() throws Exception {
    MailConsumerImp mailConsumer = createConsumerForTests(gm);
    assertNull(mailConsumer.getAttemptConnectOnInit());
    assertTrue(mailConsumer.attemptConnectOnInit());

    mailConsumer.setAttemptConnectOnInit(Boolean.FALSE);
    assertEquals(Boolean.FALSE, mailConsumer.getAttemptConnectOnInit());
    assertFalse(mailConsumer.attemptConnectOnInit());

    mailConsumer.setAttemptConnectOnInit(null);
    assertNull(mailConsumer.getAttemptConnectOnInit());
    assertTrue(mailConsumer.attemptConnectOnInit());
  }

  @Test
  public void testInit_AttemptConnectOnInitTrue() throws Exception {
      MailConsumerImp mailConsumer = createConsumerForTests(gm);
      MockMessageProducer mockProducer = new MockMessageProducer();
      mailConsumer.setAttemptConnectOnInit(Boolean.TRUE);
      Adapter a = createAdapter(mailConsumer, mockProducer);
      a.requestInit();
      a.requestClose();
  }

  @Test
  public void testRedmine_7604() throws Exception {
    testInit_AttemptConnectOnInitFalse();
  }

  @Test
  public void testInit_AttemptConnectOnInitFalse() throws Exception {
    assumeTrue(testsEnabled());
    GreenMail gm = JunitMailHelper.startServer(JunitMailHelper.DEFAULT_RECEIVER, DEFAULT_POP3_USER,
        DEFAULT_POP3_PASSWORD);
    try {
      // It is stopped, shouldn't matter if we init, because we shouldn't try the connection
      MailConsumerImp mailConsumer = createConsumerForTests(gm);
      JunitMailHelper.stopServer(gm);
      MockMessageProducer mockProducer = new MockMessageProducer();
      mailConsumer.setAttemptConnectOnInit(Boolean.FALSE);
      Adapter a = createAdapter(mailConsumer, mockProducer);
      a.requestInit();
      a.requestClose();
    } finally {
    }
  }

  protected void sendMessage(GreenMail gm) throws Exception {
    sendMessage(gm, 1);
  }

  protected void sendMessage(GreenMail gm, int numPayloads) throws Exception {
    Session session = Session.getDefaultInstance(new Properties());
    MimeMessage mimeMessage = new MimeMessage(session);
    mimeMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(JunitMailHelper.DEFAULT_RECEIVER));
    mimeMessage.setFrom(InternetAddress.parse(JunitMailHelper.DEFAULT_SENDER)[0]);
    mimeMessage.setContent(new MimeMultipart());

    for (int i=0;i<numPayloads;i++){
      String payload = TEXT_PAYLOADS[i % TEXT_PAYLOADS.length];
      if (i > TEXT_PAYLOADS.length){
        payload += " " + i / TEXT_PAYLOADS.length;
      }
      attachPayload(mimeMessage, payload, "text/plain");
    }

    mimeMessage.setSubject(DEFAULT_SUBJECT);
    mimeMessage.setSentDate(new Date());
    mimeMessage.saveChanges();
    GreenMailUser user = gm.setUser(JunitMailHelper.DEFAULT_RECEIVER, DEFAULT_POP3_USER, DEFAULT_POP3_PASSWORD);
    user.deliver(mimeMessage);
  }

  protected MailConsumerImp createConsumerForTests(Pop3Server server, MailReceiverFactory factory) {
    String pop3Url = server.getProtocol() + "://localhost:" + server.getPort() + "/INBOX";
    MailConsumerImp consumer = create();
    consumer.setMailReceiverFactory(factory);
    consumer.setDeleteOnReceive(true);
    consumer.setPoller(new FixedIntervalPoller(new TimeInterval(300L, TimeUnit.MILLISECONDS)));
    consumer.setReacquireLockBetweenMessages(true);
    consumer.setRegularExpressionStyle(REGEX_STYLE);
    consumer.setMailboxUrl(pop3Url);
    consumer.setFilterExpression("SUBJECT=.*Junit Test.*");
    consumer.setUsername(DEFAULT_POP3_USER);
    consumer.setPassword(DEFAULT_ENCODED_POP3_PASSWORD);
    return consumer;
  }

  protected MailConsumerImp createConsumerForTests(GreenMail gm, MailReceiverFactory factory) {
    return createConsumerForTests(gm.getPop3(), factory);
  }


  protected MailConsumerImp createConsumerForTests(GreenMail gm) {
    return createConsumerForTests(gm, new JavamailReceiverFactory());
  }

  protected abstract MailConsumerImp create();

  protected MailConsumerImp create(Poller pollerImp) {
    MailConsumerImp impl = create();
    impl.setPoller(pollerImp);
    return impl;
  }

  /**
   * @see com.adaptris.core.ExampleConfigCase#retrieveObjectForSampleConfig()
   */
  @Override
  protected Object retrieveObjectForSampleConfig() {
    return null;
  }

  /**
   * @see com.adaptris.core.ExampleConfigCase#retrieveObjectForSampleConfig()
   */
  @Override
  protected List<StandaloneConsumer> retrieveObjectsForSampleConfig() {
    List<StandaloneConsumer> result = new ArrayList<>();

    MailConsumerImp consumer = create(new QuartzCronPoller("00 */10 * * * ?"));
    result.add(new StandaloneConsumer(configure("pop3://server:110/INBOX", consumer)));

    consumer = create(new FixedIntervalPoller(new TimeInterval(10L, TimeUnit.MINUTES)));
    result.add(new StandaloneConsumer(configure("pop3://server:110/INBOX", consumer)));


    consumer = create(new QuartzCronPoller("00 */10 * * * ?"));
    result.add(new StandaloneConsumer(configure("pop3s://pop.gmail.com:995/INBOX", consumer)));

    consumer = create(new FixedIntervalPoller(new TimeInterval(10L, TimeUnit.MINUTES)));
    result.add(new StandaloneConsumer(configure("pop3s://pop.gmail.com:995/INBOX", consumer)));

    consumer = create(new QuartzCronPoller("00 */10 * * * ?"));
    result.add(new StandaloneConsumer(configure("imaps://imap.gmail.com:993/INBOX", consumer)));

    consumer = create(new FixedIntervalPoller(new TimeInterval(10L, TimeUnit.MINUTES)));
    result.add(new StandaloneConsumer(configure("imaps://imap.gmail.com:993/INBOX", consumer)));

    consumer = create(new QuartzCronPoller("00 */10 * * * ?"));
    result.add(new StandaloneConsumer(configure("imap://my.imap.server:143/INBOX", consumer)));

    consumer = create(new FixedIntervalPoller(new TimeInterval(10L, TimeUnit.MINUTES)));
    result.add(new StandaloneConsumer(configure("imap://my.imap.server:143/INBOX", consumer)));

    return result;
  }

  @Override
  protected String createBaseFileName(Object object) {
    String basename = super.createBaseFileName(object);
    StandaloneConsumer c = (StandaloneConsumer) object;
    String s = ((MailConsumerImp) c.getConsumer()).getMailboxUrl();
    int pos = s.indexOf(":");
    if (pos > 0) {
      basename = basename + "-" + s.substring(0, pos).toUpperCase();
    }
    return basename;
  }

  private void attachPayload(MimeMessage msg, String payload, String contentType) throws MessagingException, IOException {
    MimeMultipart multiPart = (MimeMultipart)msg.getContent();
    MimeBodyPart part = new MimeBodyPart(new InternetHeaders(), payload.getBytes());
    part.setHeader(Mail.CONTENT_TYPE, contentType);
    multiPart.addBodyPart(part);
  }

  public Adapter createAdapter(MailConsumerImp consumer, AdaptrisMessageProducer producer) throws Exception {
    return createAdapter(createWorkflow(consumer, producer));
  }

  protected Adapter createAdapter(Workflow w) throws Exception {
    Adapter a = new Adapter();
    a.setUniqueId(new GuidGenerator().getUUID());
    ChannelList cl = new ChannelList();
    Channel channel = new Channel();
    channel.setConsumeConnection(new NullConnection());
    WorkflowList wfl = new WorkflowList();
    wfl.getWorkflows().add(w);
    channel.setWorkflowList(wfl);
    cl.addChannel(channel);
    a.setChannelList(cl);
    return a;
  }

  protected Workflow createWorkflow(MailConsumerImp consumer, AdaptrisMessageProducer producer) {
    StandardWorkflow wf = new StandardWorkflow();
    wf.setConsumer(consumer);
    wf.setProducer(producer);
    wf.setServiceCollection(new ServiceList());
    return wf;
  }

  MailConsumerImp configure(String destination, MailConsumerImp impl) {
    JavamailReceiverFactory fac = new JavamailReceiverFactory();
    fac.getSessionProperties().addKeyValuePair(new KeyValuePair("mail.smtp.starttls.enable", "true"));
    fac.getSessionProperties().addKeyValuePair(new KeyValuePair("mail.pop3.starttls.enable", "true"));
    impl.setMailboxUrl(destination);
    impl.setFilterExpression("FROM=optionalFilter,SUBJECT=optionalFilter,RECIPIENT=optionalFilter");
    impl.setUsername("myUsername");
    impl.setPassword("MyPassword which might be encoded");
    return impl;

  }
}
