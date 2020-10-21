package com.adaptris.core.mail;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.MultiPayloadMessageFactory;
import com.adaptris.core.StandaloneConsumer;
import com.adaptris.core.stubs.MockMessageListener;
import com.adaptris.core.util.LifecycleHelper;
import com.adaptris.interlok.junit.scaffolding.ExampleConsumerCase;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.List;
import java.util.Properties;

public class O365MailConsumerTest extends ExampleConsumerCase
{
  private O365MailConsumer consumer;

  @Before
  public void setUp() throws IOException
  {
    consumer = new O365MailConsumer();

    Properties properties = new Properties();
    properties.load(new FileInputStream(this.getClass().getResource("o365.properties").getFile()));

    consumer.setApplicationId(properties.getProperty("APPLICATION_ID"));
    consumer.setTenantId(properties.getProperty("TENANT_ID"));
    consumer.setClientSecret(properties.getProperty("CLIENT_SECRET"));
    consumer.setUsername(properties.getProperty("USERNAME"));

    consumer.setMessageFactory(new MultiPayloadMessageFactory());
  }

  @Test
  public void testConsumer() throws Exception
  {
    MockMessageListener mockMessageListener = new MockMessageListener(10);
    StandaloneConsumer standaloneConsumer = new StandaloneConsumer(consumer);
    standaloneConsumer.registerAdaptrisMessageListener(mockMessageListener);
    try
    {
      LifecycleHelper.init(standaloneConsumer);
      LifecycleHelper.prepare(standaloneConsumer);
      LifecycleHelper.start(standaloneConsumer);

      waitForMessages(mockMessageListener, 5, 5000); // wait until we get five new emails or for 5 seconds

      List<AdaptrisMessage> messages = mockMessageListener.getMessages();

      System.out.println("Found " + messages.size() + " emails");
      Thread.sleep(5000); // sleep for 5 seconds, otherwise the Graph SDK complains we disconnected while waiting for a response
    }
    catch (InterruptedIOException | InterruptedException e)
    {
      // Ignore these as they're occasionally thrown by the Graph SDK teh connection is closed while it's still processing
    }
    finally
    {
      stop(standaloneConsumer);
    }
  }

  @Override
  protected Object retrieveObjectForSampleConfig()
  {
    return consumer;
  }
}
