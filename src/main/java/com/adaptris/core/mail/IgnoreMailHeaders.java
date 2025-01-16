/*
 * Copyright 2017 Adaptris Ltd.
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

import jakarta.mail.internet.MimeMessage;

import com.adaptris.core.AdaptrisMessage;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Do nothing with mail headers.
 * 
 * @author lchan
 *
 */
@XStreamAlias("ignore-mail-headers")
public class IgnoreMailHeaders implements MailHeaderHandler {

  @Override
  public void handle(MimeMessage mime, AdaptrisMessage msg) {
  }

}
