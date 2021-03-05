package com.adaptris.core.mail;

import javax.mail.URLName;

import org.apache.commons.lang3.StringUtils;

public class URLNameHelper {

  private URLNameHelper() {
  }

  /**
   * A toString method that stringify the URLName but hide the password.
   * It's very similar to {@link URLName#toString()} but it's meant to hide the password for logging.
   * It also does not encode the username.
   *
   * @param url URLName
   * @return url safe string
   */
  public static String toSafeString(URLName url) {
    StringBuilder tempURL = new StringBuilder();

    addProtocol(url, tempURL);

    // Empty host are allowed (file:///)
    if (StringUtils.isNotBlank(url.getUsername()) || url.getHost() != null) {
      tempURL.append("//");

      addCredentials(url, tempURL);
      addHost(url, tempURL);
      addPort(url, tempURL);

      if (StringUtils.isNotBlank(url.getFile())) {
        tempURL.append("/");
      }
    }

    addFile(url, tempURL);
    addRef(url, tempURL);
    return tempURL.toString();
  }

  private static void addProtocol(URLName url, StringBuilder tempURL) {
    if (StringUtils.isNotBlank(url.getProtocol())) {
      tempURL.append(url.getProtocol());
      tempURL.append(":");
    }
  }

  private static void addCredentials(URLName url, StringBuilder tempURL) {
    if (StringUtils.isNotBlank(url.getUsername())) {
      tempURL.append(url.getUsername());
      if (StringUtils.isNotBlank(url.getPassword())) {
        tempURL.append(":");
        tempURL.append("*****");
      }
      tempURL.append("@");
    }
  }

  private static void addHost(URLName url, StringBuilder tempURL) {
    if (StringUtils.isNotBlank(url.getHost())) {
      tempURL.append(url.getHost());
    }
  }

  private static void addPort(URLName url, StringBuilder tempURL) {
    if (url.getPort() != -1) {
      tempURL.append(":");
      tempURL.append(Integer.toString(url.getPort()));
    }
  }

  private static void addFile(URLName url, StringBuilder tempURL) {
    if (StringUtils.isNotBlank(url.getFile())) {
      tempURL.append(url.getFile());
    }
  }

  private static void addRef(URLName url, StringBuilder tempURL) {
    if (StringUtils.isNotBlank(url.getRef())) {
      tempURL.append("#");
      tempURL.append(url.getRef());
    }
  }

}
