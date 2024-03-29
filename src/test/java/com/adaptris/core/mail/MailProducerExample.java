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

import static com.adaptris.mail.JunitMailHelper.startServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import com.adaptris.interlok.junit.scaffolding.ExampleProducerCase;
import com.adaptris.mail.JunitMailHelper;
import com.icegreen.greenmail.util.GreenMail;

public abstract class MailProducerExample extends ExampleProducerCase {

  /**
   * Key in unit-test.properties that defines where example goes unless overriden {@link #setBaseDir(String)}.
   *
   */
  public static final String BASE_DIR_KEY = "MailProducerExamples.baseDir";

  public MailProducerExample() {
    if (PROPERTIES.getProperty(BASE_DIR_KEY) != null) {
      setBaseDir(PROPERTIES.getProperty(BASE_DIR_KEY));
    }
  }

  private static GreenMail gm;

  @BeforeAll
  public static void setupGreenmail() throws Exception {
    gm = startServer();
  }

  @AfterAll
  public static void tearDownGreenmail() throws Exception {
    JunitMailHelper.stopServer(gm);
  }

  @BeforeEach
  public void beforeMailTests() throws Exception {
    gm.purgeEmailFromAllMailboxes();
  }

  protected static GreenMail mailServer() {
    return gm;
  }

}
