package com.adaptris.core.mail;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;
import com.adaptris.core.NullConnection;
import com.adaptris.core.StandaloneProducer;
import com.adaptris.interlok.junit.scaffolding.ExampleConsumerCase;
import com.adaptris.interlok.junit.scaffolding.services.ExampleServiceCase;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class O365MailProducerTest extends ExampleConsumerCase
{
  private static final String SUBJECT = "InterlokMail Office365 Test Message";
  private static final String MESSAGE = "Bacon ipsum dolor amet tail landjaeger ribeye sausage, prosciutto pork belly strip steak pork loin pork bacon biltong ham hock leberkas boudin chicken. Brisket sirloin ground round, drumstick cupim rump chislic tongue short loin pastrami bresaola pork belly alcatra spare ribs buffalo. Swine chuck frankfurter pancetta. Corned beef spare ribs pork kielbasa, chuck jerky t-bone ground round burgdoggen.";

  private O365MailProducer producer;

  @Before
  public void setUp() throws IOException
  {
    producer = new O365MailProducer();

    Properties properties = new Properties();
    properties.load(new FileInputStream(this.getClass().getResource("o365.properties").getFile()));

    producer.setApplicationId(properties.getProperty("APPLICATION_ID"));
    producer.setTenantId(properties.getProperty("TENANT_ID"));
    producer.setClientSecret(properties.getProperty("CLIENT_SECRET"));
    producer.setUsername(properties.getProperty("USERNAME"));

    producer.setSubject(SUBJECT);
    producer.setToRecipients(properties.getProperty("USERNAME")); // send it to ourself so we're not spamming anyone else
    producer.setSave(true);
  }

  @Test
  public void testProducer() throws Exception
  {
    AdaptrisMessage message = AdaptrisMessageFactory.getDefaultInstance().newMessage(MESSAGE);
    StandaloneProducer standaloneProducer = new StandaloneProducer(new NullConnection(), producer);
    ExampleServiceCase.execute(standaloneProducer, message);
  }

  @Override
  protected Object retrieveObjectForSampleConfig()
  {
    return producer;
  }
}
