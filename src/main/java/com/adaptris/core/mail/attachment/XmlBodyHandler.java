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

package com.adaptris.core.mail.attachment;
import static com.adaptris.util.text.xml.XPath.newXPathInstance;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeUtility;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.xml.namespace.NamespaceContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.core.util.DocumentBuilderFactoryBuilder;
import com.adaptris.util.KeyValuePairSet;
import com.adaptris.util.stream.StreamUtil;
import com.adaptris.util.text.xml.SimpleNamespaceContext;
import com.adaptris.util.text.xml.XPath;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Handle the body for {@link MultiAttachmentSmtpProducer} where the XPath should be treated as an XML document.
 * <p>
 * If the {@code DocumentBuilderFactoryBuilder} has been explicitly set to be not namespace aware and the document does in fact
 * contain namespaces, then Saxon can cause merry havoc in the sense that {@code //NonNamespaceXpath} doesn't work if the document
 * has namespaces in it. We have included a shim so that behaviour can be toggled based on what you have configured.
 * </p>
 *
 * @see XPath#newXPathInstance(DocumentBuilderFactoryBuilder, NamespaceContext)
 *
 * @config mail-xml-body-handler
 *
 */
@XStreamAlias("mail-xml-body-handler")
public class XmlBodyHandler implements BodyHandler {
  @NotNull
  @NotBlank
  private String xpath;
  private String contentType;
  private String encodingXpath;
  private KeyValuePairSet namespaceContext;
  @AdvancedConfig
  @Valid
  private DocumentBuilderFactoryBuilder xmlDocumentFactoryConfig;

  public XmlBodyHandler() {

  }

  public XmlBodyHandler(String xpath, String contentType) {
    this();
    setXpath(xpath);
    setContentType(contentType);
  }

  public XmlBodyHandler(String xpath, String contentType, String encoding) {
    this(xpath, contentType);
    setEncodingXpath(encoding);
  }

  /**
   * @return the xpath
   */
  public String getXpath() {
    return xpath;
  }

  /**
   * The XPath to find the body of the message.
   *
   * @param s the xpath to set
   */
  public void setXpath(String s) {
    xpath = s;
  }

  /**
   *
   * @see com.adaptris.core.mail.attachment.BodyHandler#resolve(org.w3c.dom.Document)
   */
  @Override
  public MailContent resolve(Document doc) throws Exception {
    DocumentBuilderFactoryBuilder builder = documentFactoryBuilder();
    XPath x = newXPathInstance(builder, SimpleNamespaceContext.create(getNamespaceContext()));
    Node n = x.selectSingleNode(doc, getXpath());
    MailContent result = new MailContent(getData(n), new ContentType(getContentType()));
    return result;
  }

  protected byte[] getData(Node n) throws Exception {
    if (n == null) {
      return new byte[0];
    }
    XPath x = new XPath(SimpleNamespaceContext.create(getNamespaceContext()));
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    n.normalize();
    String s = n.getTextContent();
    ByteArrayInputStream in = new ByteArrayInputStream(s.getBytes());
    InputStream encodedIn = in;
    if (getEncodingXpath() != null) {
      String encoding = x.selectSingleTextItem(n, getEncodingXpath());
      encodedIn = MimeUtility.decode(in, encoding);
    }
    StreamUtil.copyAndClose(encodedIn, out);
    return out.toByteArray();
  }

  /**
   * @return the contentType
   */
  public String getContentType() {
    return contentType;
  }

  /**
   * Set the content type that will be used for the body.
   *
   * @param contentType the contentType to set
   */
  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  /**
   * @return the encoding
   */
  public String getEncodingXpath() {
    return encodingXpath;
  }

  /**
   * If specified the value returned by the xpath will be used to decode the contents of the Body xpath.
   *
   * @param encoding the encoding to set
   */
  public void setEncodingXpath(String encoding) {
    encodingXpath = encoding;
  }

  /**
   * @return the namespaceContext
   */
  public KeyValuePairSet getNamespaceContext() {
    return namespaceContext;
  }

  /**
   * Set the namespace context for resolving namespaces.
   * <ul>
   * <li>The key is the namespace prefix</li>
   * <li>The value is the namespace uri</li>
   * </ul>
   *
   * @param kvps the namespace context
   * @see SimpleNamespaceContext#create(KeyValuePairSet)
   */
  public void setNamespaceContext(KeyValuePairSet kvps) {
    namespaceContext = kvps;
  }

  public DocumentBuilderFactoryBuilder getXmlDocumentFactoryConfig() {
    return xmlDocumentFactoryConfig;
  }

  public void setXmlDocumentFactoryConfig(DocumentBuilderFactoryBuilder xml) {
    xmlDocumentFactoryConfig = xml;
  }

  DocumentBuilderFactoryBuilder documentFactoryBuilder() {
    return DocumentBuilderFactoryBuilder.newInstanceIfNull(getXmlDocumentFactoryConfig());
  }
}
