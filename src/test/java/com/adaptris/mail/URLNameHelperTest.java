package com.adaptris.mail;

import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;

import javax.mail.URLName;

import org.junit.Test;

import com.adaptris.core.mail.URLNameHelper;

public class URLNameHelperTest {

  @Test
  public void testToSafeString() throws MalformedURLException {
    URLName url = new URLName("smtp://smtp.gmail.com:587");

    assertEquals("smtp://smtp.gmail.com:587", URLNameHelper.toSafeString(url));
  }

  @Test
  public void testToSafeStringNoPort() throws MalformedURLException {
    URLName url = new URLName("smtp://smtp.gmail.com");

    assertEquals("smtp://smtp.gmail.com", URLNameHelper.toSafeString(url));
  }

  @Test
  public void testToSafeStringNoProtocolNoPort() throws MalformedURLException {
    URLName url = new URLName("smtp.gmail.com");

    assertEquals("smtp.gmail.com", URLNameHelper.toSafeString(url));
  }

  @Test
  public void testToSafeStringHiddenPassword() throws MalformedURLException {
    URLName url = new URLName("smtp://username:MySecretPassword@smtp.gmail.com:587");

    assertEquals("smtp://username:*****@smtp.gmail.com:587", URLNameHelper.toSafeString(url));
  }

  @Test
  public void testToSafeStringUsernameNotEncoded() throws MalformedURLException {
    URLName url = new URLName("smtp://username%40host.local@smtp.gmail.com:587");

    assertEquals("smtp://username@host.local@smtp.gmail.com:587", URLNameHelper.toSafeString(url));
  }

  @Test
  public void testToSafeStringNoHost() throws MalformedURLException {
    URLName url = new URLName("file:/path/to/file");

    assertEquals("file:/path/to/file", URLNameHelper.toSafeString(url));

    url = new URLName("file:///path/to/file");

    assertEquals("file:///path/to/file", URLNameHelper.toSafeString(url));
  }

  @Test
  public void testToSafeStringWithRef() throws MalformedURLException {
    URLName url = new URLName("http://host.local/context#ref");

    assertEquals("http://host.local/context#ref", URLNameHelper.toSafeString(url));
  }

}
