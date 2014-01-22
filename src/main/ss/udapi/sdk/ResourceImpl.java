package ss.udapi.sdk;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.log4j.Logger;

import com.rabbitmq.client.ConnectionFactory;

import ss.udapi.sdk.interfaces.Resource;
import ss.udapi.sdk.model.RestItem;
import ss.udapi.sdk.model.ServiceRequest;
import ss.udapi.sdk.model.StreamEcho;
import ss.udapi.sdk.model.Summary;
import ss.udapi.sdk.services.EchoSender;
import ss.udapi.sdk.services.HttpServices;
import ss.udapi.sdk.services.JsonHelper;
import ss.udapi.sdk.services.MQListener;
import ss.udapi.sdk.services.SystemProperties;
import ss.udapi.sdk.streaming.Event;

public class ResourceImpl implements Resource
{
  private Logger logger = Logger.getLogger(ResourceImpl.class.getName());
  
  private ServiceRequest availableResources;
  private RestItem restItem = new RestItem();
  private static HttpServices httpSvcs = new HttpServices();
  
  private int echoSenderInterval;
  private int maxMissedEchos;
  private List<Event> streamingEvents;
  
  
  
  protected ResourceImpl(RestItem restItem, ServiceRequest availableResources){
    this.restItem = restItem;
    this.availableResources = availableResources;
    logger.debug("Instantiated Resource: " + restItem.getName());
  }
  

  @Override
  public String getSnapshot()
  {
    return httpSvcs.getSnapshot(availableResources, "http://api.sportingsolutions.com/rels/snapshot", restItem.getName());
  }

  @Override
  public void startStreaming(List<Event> events)
  {
    startStreaming(events,
              new Integer(SystemProperties.get("ss.echo_sender_interval")),
              new Integer(SystemProperties.get("ss.echo_max_missed_echos")));
  }
  

  private void startStreaming(List<Event> events, int echoSenderInterval, int maxMissedEchos)
  {
    logger.info(String.format("Starting stream for %1$s with Echo Interval of %2$s and Max Missed Echos of %3$s",getName(),echoSenderInterval,maxMissedEchos));
    this.streamingEvents = events;
    this.echoSenderInterval = echoSenderInterval;
    this.maxMissedEchos = maxMissedEchos;
  
    streamData();
  }

  
  
  private void streamData()
  {
    ServiceRequest amqpRequest = new ServiceRequest();
    amqpRequest = httpSvcs.processRequest(availableResources,"http://api.sportingsolutions.com/rels/stream/amqp", restItem.getName());
    
    String amqpURI = amqpRequest.getServiceRestItems().get(0).getLinks().get(0).getHref();
    System.out.println("------------>Starting new streaming services: name " + restItem.getName() + " with queue " + amqpURI);
    
    
    //move to a separate static threadpool so we don't create three threads per resource :-)
    Executor exec = Executors.newFixedThreadPool(3);
  
    
    MQListener mqListener = MQListener.getMQListener(amqpURI, availableResources);
    
//    MQListener mqListener = MQListener.getMQListener(amqpURI, availableResources);
    
//    if (mqListener.isRunning() == false)
    //{
      exec.execute(mqListener);
    //}
    
    
    EchoSender echoSender = EchoSender.getEchoSender(amqpURI, availableResources);
    exec.execute(echoSender);

    
    
    
  }

  
  
  
  
  

  @Override
  public void stopStreaming()
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void pauseStreaming()
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void unpauseStreaming()
  {
    // TODO Auto-generated method stub
  }

  
  @Override
  public String getId()
  {
    return restItem.getContent().getId();
  }

  @Override
  public String getName()
  {
    return restItem.getName();
  }

  @Override
  public Summary getContent()
  {
    return restItem.getContent();
  }


  
  

  
  
  
}











