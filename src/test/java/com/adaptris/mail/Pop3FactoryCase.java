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

import static com.adaptris.mail.JunitMailHelper.DEFAULT_RECEIVER;
import static com.adaptris.mail.JunitMailHelper.startServer;
import static com.adaptris.mail.JunitMailHelper.testsEnabled;
import static com.adaptris.mail.MailReceiverCase.DEFAULT_POP3_PASSWORD;
import static com.adaptris.mail.MailReceiverCase.DEFAULT_POP3_USER;
import static com.adaptris.mail.MailReceiverCase.createURLName;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import javax.mail.URLName;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.adaptris.interlok.junit.scaffolding.BaseCase;
import com.icegreen.greenmail.pop3.Pop3Server;
import com.icegreen.greenmail.util.GreenMail;

@SuppressWarnings("deprecation")
public abstract class Pop3FactoryCase extends BaseCase {

  abstract Pop3ReceiverFactory create();

  abstract Pop3Server getServer(GreenMail gm);

  abstract Pop3ReceiverFactory configure(Pop3ReceiverFactory f);

  private static GreenMail greenmail;

  @BeforeAll
  public static void setupGreenmail() throws Exception {
    greenmail = startServer(DEFAULT_RECEIVER, DEFAULT_POP3_USER, DEFAULT_POP3_PASSWORD);
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

  protected static GreenMail mailServer() {
    return greenmail;
  }


  @Test
  public void testCreate() throws Exception {
    Pop3ReceiverFactory fac = create();
    Pop3Server server = getServer(greenmail);
    String pop3UrlString = server.getProtocol() + "://localhost:" + server.getPort() + "/INBOX";
    URLName pop3Url = createURLName(pop3UrlString, DEFAULT_POP3_USER, DEFAULT_POP3_PASSWORD);
    assertNotNull(fac.createClient(pop3Url));
  }

  @Test
  public void testCreate_NotSupported() throws Exception {
    Pop3ReceiverFactory fac = create();
    Pop3Server server = getServer(greenmail);
    String pop3UrlString = "imap://localhost:" + server.getPort() + "/INBOX";
    URLName pop3Url = createURLName(pop3UrlString, DEFAULT_POP3_USER, DEFAULT_POP3_PASSWORD);
    assertThrows(MailException.class, ()->{
      fac.createClient(pop3Url);
    }, "Failed to create, not supported");
  }

  @Test
  public void testCreate_Connect() throws Exception {
    Pop3ReceiverFactory fac = create();
    Pop3Server server = getServer(greenmail);
    String pop3UrlString = server.getProtocol() + "://localhost:" + server.getPort() + "/INBOX";
    URLName pop3Url = createURLName(pop3UrlString, DEFAULT_POP3_USER, DEFAULT_POP3_PASSWORD);
    MailReceiver client = fac.createClient(pop3Url);
    try {
      client.connect();
    } finally {
      IOUtils.closeQuietly(client);
    }
  }

  @Test
  public void testCreate_Configure_Connect() throws Exception {
    // Handshake Exceptions happenin...
    // It is consistently this test, but Greemail is "static" so there's only
    // one instance.
    // Force it to be skipped to avoid jenkins issues.
    assumeTrue(false);
    Pop3ReceiverFactory fac = configure(create());
    Pop3Server server = getServer(greenmail);
    String pop3UrlString = server.getProtocol() + "://localhost:" + server.getPort() + "/INBOX";
    URLName pop3Url = createURLName(pop3UrlString, DEFAULT_POP3_USER, DEFAULT_POP3_PASSWORD);
    MailReceiver client = fac.createClient(pop3Url);
    try {
      client.connect();
    } finally {
      IOUtils.closeQuietly(client);
    }
  }

}
