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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import jakarta.mail.internet.MimeBodyPart;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.StandaloneConsumer;
import com.adaptris.core.stubs.MockMessageListener;
import com.adaptris.core.util.LifecycleHelper;
import com.adaptris.core.util.MimeHelper;
import com.adaptris.mail.JunitMailHelper;
import com.adaptris.mail.Pop3ReceiverFactory;
import com.adaptris.util.text.mime.BodyPartIterator;

public class RawMailConsumerTest extends MailConsumerCase {


  @Test
  public void testConsumer() throws Exception {
    sendMessage(mailServer());
    MockMessageListener mockListener = new MockMessageListener();
    RawMailConsumer imp = (RawMailConsumer) createConsumerForTests(mailServer());
    imp.setUseEmailMessageIdAsUniqueId(true);
    imp.setDeleteOnReceive(null);
    StandaloneConsumer c = new StandaloneConsumer(imp);
    c.registerAdaptrisMessageListener(mockListener);
    LifecycleHelper.initAndStart(c);
    waitForMessages(mockListener, 1);
    LifecycleHelper.stopAndClose(c);
    assertTrue(mockListener.getMessages().size() >= 1);
    compare(mockListener.getMessages().get(0), TEXT_PAYLOADS[0]);

  }

  @Test
  public void testConsumer_MetadataHeaders() throws Exception {
    sendMessage(mailServer());
    MockMessageListener mockListener = new MockMessageListener();
    MailConsumerImp imp = createConsumerForTests(mailServer());
    imp.setHeaderHandler(new MetadataMailHeaders());
    StandaloneConsumer c = new StandaloneConsumer(imp);
    c.registerAdaptrisMessageListener(mockListener);
    LifecycleHelper.initAndStart(c);
    waitForMessages(mockListener, 1);
    LifecycleHelper.stopAndClose(c);
    AdaptrisMessage prdMsg = mockListener.getMessages().get(0);
    compare(prdMsg, TEXT_PAYLOADS[0]);
    assertEquals(JunitMailHelper.DEFAULT_RECEIVER, prdMsg.getMetadataValue("To"));

  }

  @Test
  public void testConsumer_CommonsNet() throws Exception {
    sendMessage(mailServer());
    MockMessageListener mockListener = new MockMessageListener();
    StandaloneConsumer c =
        new StandaloneConsumer(createConsumerForTests(mailServer(), new Pop3ReceiverFactory()));
    c.registerAdaptrisMessageListener(mockListener);
    LifecycleHelper.initAndStart(c);
    waitForMessages(mockListener, 1);
    LifecycleHelper.stopAndClose(c);

    assertTrue(mockListener.getMessages().size() >= 1);
    compare(mockListener.getMessages().get(0), TEXT_PAYLOADS[0]);

  }

  @Override
  protected RawMailConsumer create() {
    return new RawMailConsumer();
  }

  private void compare(AdaptrisMessage msg, String expected) throws Exception {
    try (InputStream msgIn = msg.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      BodyPartIterator mime = MimeHelper.createBodyPartIterator(msg);
      MimeBodyPart part = mime.next();
      try (InputStream partIn = part.getInputStream(); OutputStream bout = out) {
        IOUtils.copy(partIn, bout);
      }
      assertEquals(expected, out.toString());
    }

  }
}
