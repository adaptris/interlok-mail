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

import static com.adaptris.mail.MailReceiverCase.DEFAULT_POP3_PASSWORD;
import static com.adaptris.mail.MailReceiverCase.DEFAULT_POP3_USER;
import static com.adaptris.mail.MailReceiverCase.createURLName;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.mail.URLName;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import com.icegreen.greenmail.pop3.Pop3Server;
import com.icegreen.greenmail.util.GreenMail;

@SuppressWarnings("deprecation")
public class Pop3sReceiverFactoryTest extends Pop3FactoryCase {

  @Override
  Pop3sReceiverFactory create() {
    return new Pop3sReceiverFactory();
  }

  @Override
  Pop3Server getServer(GreenMail gm) {
    return gm.getPop3s();
  }

  // By default this will *fail* because we don't have a trusted certificate.
  @Override
  @Test
  public void testCreate_Connect() throws Exception {
    Pop3sReceiverFactory fac = create();
    Pop3Server server = getServer(mailServer());
    String pop3UrlString = server.getProtocol() + "://localhost:" + server.getPort() + "/INBOX";
    URLName pop3Url = createURLName(pop3UrlString, DEFAULT_POP3_USER, DEFAULT_POP3_PASSWORD);
    MailReceiver client = fac.createClient(pop3Url);
    try {
      assertThrows(MailException.class, ()->{
        client.connect();
      }, "Failed to connect, no trusted cert");
    }
    finally {
      IOUtils.closeQuietly(client);
    }
  }

  @Override
  Pop3sReceiverFactory configure(Pop3ReceiverFactory f) {
    Pop3sReceiverFactory fac = (Pop3sReceiverFactory) f;
    f.setConnectTimeout(60000);
    f.setKeepAlive(true);
    f.setReceiveBufferSize(8192);
    f.setSendBufferSize(8192);
    f.setTcpNoDelay(true);
    f.setTimeout(60000);
    fac.setImplicitTls(true);
    fac.setAlwaysTrust(true);
    fac.setCipherSuites("TLS_DHE_DSS_WITH_AES_128_CBC_SHA,TLS_DHE_RSA_WITH_AES_256_CBC_SHA,TLS_RSA_WITH_AES_256_CBC_SHA");
    fac.setProtocols("SSLv3,TLSv1,TLSv1.1,SSLv2Hello");
    return fac;
  }
}
