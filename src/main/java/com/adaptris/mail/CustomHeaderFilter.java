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

import java.util.Arrays;
import java.util.List;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;

import org.apache.commons.lang3.ArrayUtils;

import com.adaptris.core.util.Args;


/**
 * Filter on a Custom Header
 * 
 * @author lchan
 * @author $Author: lchan $
 */
class CustomHeaderFilter extends MessageFilterImp {

  private String customHeader;

  CustomHeaderFilter(MatchProxy m, String hdr) {
    super(m);
    customHeader = Args.notBlank(hdr, "customHeader");
  }

  @Override
  List<String> getHeaders(Message m) throws MessagingException {
    return Arrays.asList(ArrayUtils.nullToEmpty(m.getHeader(customHeader)));
  }
}
