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

import static com.adaptris.mail.JunitMailHelper.DEFAULT_RECEIVER;
import static com.adaptris.mail.JunitMailHelper.startServer;
import static com.adaptris.mail.JunitMailHelper.stopServer;
import static com.adaptris.mail.JunitMailHelper.testsEnabled;
import static com.adaptris.mail.MailReceiverCase.DEFAULT_POP3_PASSWORD;
import static com.adaptris.mail.MailReceiverCase.DEFAULT_POP3_USER;
import static com.adaptris.mail.MailReceiverCase.createURLName;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import javax.mail.URLName;
import org.apache.commons.io.IOUtils;
import org.junit.Assume;
import org.junit.Test;
import com.adaptris.core.BaseCase;
import com.icegreen.greenmail.pop3.Pop3Server;
import com.icegreen.greenmail.util.GreenMail;

@SuppressWarnings("deprecation")
public abstract class Pop3FactoryCase extends BaseCase {

  abstract Pop3ReceiverFactory create();

  abstract Pop3Server getServer(GreenMail gm);

  abstract Pop3ReceiverFactory configure(Pop3ReceiverFactory f);

  @Test
  public void testCreate() throws Exception {
    Assume.assumeTrue(testsEnabled());
    GreenMail gm = startServer(DEFAULT_RECEIVER, DEFAULT_POP3_USER, DEFAULT_POP3_PASSWORD);
    try {
      Pop3ReceiverFactory fac = create();
      Pop3Server server = getServer(gm);
      String pop3UrlString = server.getProtocol() + "://localhost:" + server.getPort() + "/INBOX";
      URLName pop3Url = createURLName(pop3UrlString, DEFAULT_POP3_USER, DEFAULT_POP3_PASSWORD);
      assertNotNull(fac.createClient(pop3Url));
    }
    finally {
      stopServer(gm);
    }
  }

  @Test
  public void testCreate_NotSupported() throws Exception {
    Assume.assumeTrue(testsEnabled());
    GreenMail gm = startServer(DEFAULT_RECEIVER, DEFAULT_POP3_USER, DEFAULT_POP3_PASSWORD);
    Pop3ReceiverFactory fac = create();
    Pop3Server server = getServer(gm);
    String pop3UrlString = "imap://localhost:" + server.getPort() + "/INBOX";
    URLName pop3Url = createURLName(pop3UrlString, DEFAULT_POP3_USER, DEFAULT_POP3_PASSWORD);
    try {
      MailReceiver client = fac.createClient(pop3Url);
      fail();
    }
    catch (MailException expected) {

    }
    finally {
      stopServer(gm);
    }
  }

  @Test
  public void testCreate_Connect() throws Exception {
    Assume.assumeTrue(testsEnabled());
    GreenMail gm = startServer(DEFAULT_RECEIVER, DEFAULT_POP3_USER, DEFAULT_POP3_PASSWORD);
    Pop3ReceiverFactory fac = create();
    Pop3Server server = getServer(gm);
    String pop3UrlString = server.getProtocol() + "://localhost:" + server.getPort() + "/INBOX";
    URLName pop3Url = createURLName(pop3UrlString, DEFAULT_POP3_USER, DEFAULT_POP3_PASSWORD);
    MailReceiver client = fac.createClient(pop3Url);
    try {
      client.connect();
    }
    finally {
      stopServer(gm);
      IOUtils.closeQuietly(client);
    }
  }

  @Test
  public void testCreate_Configure_Connect() throws Exception {
    Assume.assumeTrue(testsEnabled());
    GreenMail gm = startServer(DEFAULT_RECEIVER, DEFAULT_POP3_USER, DEFAULT_POP3_PASSWORD);
    Pop3ReceiverFactory fac = configure(create());
    Pop3Server server = getServer(gm);
    String pop3UrlString = server.getProtocol() + "://localhost:" + server.getPort() + "/INBOX";
    URLName pop3Url = createURLName(pop3UrlString, DEFAULT_POP3_USER, DEFAULT_POP3_PASSWORD);
    MailReceiver client = fac.createClient(pop3Url);
    try {
      client.connect();
    }
    finally {
      IOUtils.closeQuietly(client);
      stopServer(gm);
    }
  }

}
