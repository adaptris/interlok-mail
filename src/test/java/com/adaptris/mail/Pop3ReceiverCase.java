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
import static com.adaptris.mail.JunitMailHelper.DEFAULT_SENDER;
import static com.adaptris.mail.JunitMailHelper.assertFrom;
import static com.adaptris.mail.JunitMailHelper.assertTo;
import static org.junit.Assert.assertEquals;
import javax.mail.internet.MimeMessage;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

@SuppressWarnings("deprecation")
public abstract class Pop3ReceiverCase extends MailReceiverCase {

  @Override
  abstract MailReceiver createClient(GreenMail gm) throws Exception;

  @Test
  public void testPop3NoFilterNoDelete() throws Exception {
    ServerSetup smtpServerSetup =
        new ServerSetup(mailServer().getSmtp().getPort(), null, ServerSetup.PROTOCOL_SMTP);
    sendMessage(DEFAULT_SENDER, DEFAULT_RECEIVER, smtpServerSetup);
    sendMessage(DEFAULT_SENDER, "anotherAddress@anotherDomain.com", smtpServerSetup);
    MailReceiver mbox = createClient(mailServer());
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
      assertEquals(1, mbox.getMessages().size());
    }
    finally {
      IOUtils.closeQuietly(mbox);
    }
  }

}
