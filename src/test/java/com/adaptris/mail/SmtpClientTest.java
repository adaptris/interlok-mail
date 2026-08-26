package com.adaptris.mail;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SmtpClientTest {

  private SmtpClient client;
  
  @Mock private MimeMessage mockMessage;
  
  @Mock private Session mockSession;
  
  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
    
    client = new SmtpClient("url");
    client.setMessage(mockMessage);
    client.setSession(mockSession);
  }
  
  @Test
  public void testAddMailHeaderFailure() throws Exception {
    doThrow(new MessagingException("Expected"))
        .when(mockMessage).addHeader(anyString(), anyString());
    
    try {
      client.addMailHeader("key", "value");
      fail("Method call on the MimeMessage should throw us back an exception.");
    } catch (MailException ex) {
      //expected
    }
  }
  
  @Test
  public void testRemoveMailHeaderFailure() throws Exception {
    doThrow(new MessagingException("Expected"))
        .when(mockMessage).removeHeader(anyString());
    
    try {
      client.removeMailHeader("key");
      fail("Method call on the MimeMessage should throw us back an exception.");
    } catch (MailException ex) {
      //expected
    }
  }
  
  @Test
  public void testAddAddressToMailFailure() throws Exception {
    InternetAddress[] addresses = InternetAddress.parse("me@me.me");
    
    doThrow(new MessagingException("Expected"))
        .when(mockMessage).addRecipients(Message.RecipientType.TO, addresses);
    
    try {
      client.addTo(addresses);
      fail("Method call on the MimeMessage should throw us back an exception.");
    } catch (MailException ex) {
      //expected
    }
  }
  
  @Test
  public void testAddAddressStringCcMailFailure() throws Exception {
    InternetAddress[] addresses = InternetAddress.parse("me@me.me");
    
    doThrow(new MessagingException("Expected"))
        .when(mockMessage).addRecipients(Message.RecipientType.CC, addresses);
    
    try {
      client.addCarbonCopy(addresses);
      fail("Method call on the MimeMessage should throw us back an exception.");
    } catch (MailException ex) {
      //expected
    }
  }
  
  @Test
  public void testAddAddressStringBCcMailFailure() throws Exception {
    InternetAddress[] addresses = InternetAddress.parse("me@me.me");
    
    doThrow(new MessagingException("Expected"))
        .when(mockMessage).addRecipients(Message.RecipientType.BCC, addresses);
    
    try {
      client.addBlindCarbonCopy(addresses);
      fail("Method call on the MimeMessage should throw us back an exception.");
    } catch (MailException ex) {
      //expected
    }
  }
  
  @Test
  public void testAddAddressStringFromMailFailure() throws Exception {
    doThrow(new MessagingException("Expected"))
        .when(mockMessage).setFrom(any(InternetAddress.class));
    
    try {
      client.setFrom("me@me.me");
      fail("Method call on the MimeMessage should throw us back an exception.");
    } catch (MailException ex) {
      //expected
    }
  }
  
  @Test
  public void testSetSubjectMailFailure() throws Exception {
    doThrow(new MessagingException("Expected"))
        .when(mockMessage).setSubject(any());
    
    try {
      client.setSubject("Subject");
      fail("Method call on the MimeMessage should throw us back an exception.");
    } catch (MailException ex) {
      //expected
    }
  }
}
