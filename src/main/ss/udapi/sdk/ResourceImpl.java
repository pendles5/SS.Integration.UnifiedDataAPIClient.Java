//Copyright 2014 Spin Services Limited

//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at

//    http://www.apache.org/licenses/LICENSE-2.0

//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.

package ss.udapi.sdk;

import ss.udapi.sdk.interfaces.Resource;
import ss.udapi.sdk.model.RestItem;
import ss.udapi.sdk.model.ServiceRequest;
import ss.udapi.sdk.model.Summary;
import ss.udapi.sdk.services.EchoSender;
import ss.udapi.sdk.services.HttpServices;
import ss.udapi.sdk.services.MQListener;
import ss.udapi.sdk.services.ResourceSession;
import ss.udapi.sdk.services.ResourceWorkQueue;
import ss.udapi.sdk.services.ResourceWorkerMap;
import ss.udapi.sdk.services.ServiceThreadExecutor;
import ss.udapi.sdk.services.SystemProperties;
import ss.udapi.sdk.services.WorkQueueMonitor;
import ss.udapi.sdk.streaming.ConnectedAction;
import ss.udapi.sdk.streaming.DisconnectedAction;
import ss.udapi.sdk.streaming.Event;
import ss.udapi.sdk.streaming.StreamAction;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

/**
 * An object of this class represents an instance of a resource a.k.a fixture.
 * It provides a means of controlling monitoring of an active resource for the
 * client system(s).
 * 
 */
public class ResourceImpl implements Resource {
	
	private static Logger logger = Logger.getLogger(ResourceImpl.class.getName());
	private static HttpServices httpSvcs = new HttpServices();
	private static ExecutorService actionExecuter = Executors.newSingleThreadExecutor();

	/*
	 * This is the work queue for this resource instance. All activity for this
	 * resource's MQ queue received from Sporting Solutions end up here as well
	 * as internal echo control commands. RabbitMQConsumer and EchoSender place
	 * objects here.
	 */
	private boolean isStreaming;
	private boolean connected;
	private String amqpDest;
	private ServiceRequest availableResources;
	private RestItem restItem;
	private ResourceWorkQueue resWorkQueueRef = ResourceWorkQueue.getResourceWorkQueue();
	private List<Event> streamingEvents;

	/*
	 * Constructor initialises and resets internal state in case it is
	 * re-initialised by the client code.
	 */
	public ResourceImpl(RestItem restItem, ServiceRequest availableResources) {
		
		if (restItem == null)
			throw new IllegalArgumentException("restItem cannot be null");
		
		this.restItem = restItem;
		this.availableResources = availableResources;
		
		ResourceWorkQueue.addQueue(getId());
		logger.debug("Instantiating Resource: " + restItem.getName()
				+ " -- id: " + getId());

		if (ResourceWorkerMap.exists(getId()) == true) {
			isStreaming = true;
		} else {
			ResourceWorkerMap.addResource(getId(), this);
		}
	}

	/**
	 * Requests a full snapshot from Sporting Solutions. The snapshot will be
	 * returned via the resource's MQ queue.
	 */
	@Override
	public String getSnapshot() throws Exception {
		return httpSvcs.getSnapshot(availableResources,
				"http://api.sportingsolutions.com/rels/snapshot",
				restItem.getName());
	}

	/**
	 * Starts the monitoring for this resource's MQ queue.
	 */
	/*
	 * The monitoring is virtual. On the main, ResourceImpl instances will not
	 * be running. But logically the monitoring is done by this method, not
	 * MQListener.
	 */
	@Override
	public void startStreaming(List<Event> events) throws Exception {
		startStreaming(events,
				new Integer(SystemProperties.get("ss.echo_sender_interval")),
				new Integer(SystemProperties.get("ss.echo_max_missed_echos")));
	}

	/*
	 * This looks slightly odd as we're setting the same values we read above,
	 * but the client could set these directly so we have to allow for the
	 * values to change.
	 */
	private void startStreaming(List<Event> events, int echoSenderInterval,
			int maxMissedEchos) throws Exception {
		
		if (echoSenderInterval > 0) {
			SystemProperties.setProperty("ss.echo_max_missed_echos",
					Integer.toString(maxMissedEchos));
		}
		if (maxMissedEchos > 0) {
			SystemProperties.setProperty("ss.echo_sender_interval",
					Integer.toString(echoSenderInterval));
		}

		logger.info(String.format("Starting stream for " + getName()
				+ " with Echo Interval of: " + echoSenderInterval
				+ " and Max Missed Echos of: " + maxMissedEchos));
		
		this.streamingEvents = events;
		isStreaming = true;
		connect();
		streamData();
	}

	/*
	 * Attaches the application to the MQ service if there is no connection,
	 * otherwise it binds a new consume process to an additional queue.
	 */
	private void connect() throws Exception {

		if (connected)
			return;

		ServiceRequest amqpRequest = httpSvcs.processRequest(
				availableResources,
				"http://api.sportingsolutions.com/rels/stream/amqp",
				restItem.getName());

		if (amqpRequest == null)
			throw new Exception("Cannot connect to AMPQ queue");

		amqpDest = amqpRequest.getServiceRestItems().get(0).getLinks().get(0).getHref();
		
		logger.info("Starting new streaming service, name: " + getName()
				+ " queue: " + amqpDest + " fixture ID: " + getId());

		/*
		 * We need these three services to be running for the SDK to behave as
		 * expected. And as they need the MQ details (which are only available
		 * at this point) we are starting them here. We could retrieve and set
		 * this up earlier, but it that would mean querying the client resources
		 * twice on start up.
		 */
		WorkQueueMonitor queueWorker = WorkQueueMonitor.getMonitor();
		ServiceThreadExecutor.executeTask(queueWorker);
		MQListener listener = MQListener.getMQListener(amqpDest);
		listener.assureConnectionIsOpen();
		ServiceThreadExecutor.executeTask(listener);
		EchoSender echoSender = EchoSender.getEchoSender(amqpDest, availableResources);
		ServiceThreadExecutor.executeTask(echoSender);

		// MQListener.setResources does not allow duplicates so fall
		// through from the above false will be ignored
		MQListener.setResources(new ResourceSession(amqpDest, getId()));
		actionExecuter.execute(new ConnectedAction(streamingEvents));
		connected = true;

	}

	/**
	 * Starts callback services to the client code whenever an event is received
	 * for this resource/fixture.
	 * 
	 * This method can be called concurrently.
	 */
	public void streamData() {
		
		logger.debug("In resource " + getId() + " number of items received: "
				+ ResourceWorkQueue.size(getId()) + " streaming status: "
				+ isStreaming);
		
		StreamAction streamAction = new StreamAction(streamingEvents);
		while ((!ResourceWorkQueue.isEmpty(getId())) && (isStreaming == true)) {
			
			try {
				
				String task = resWorkQueueRef.removeUOW(getId());
				
				if(task != null) {
					logger.debug("Streaming data: " + task.substring(0, 60));
					streamAction.execute(task);
				}
				
			} catch (Exception ex) {
				logger.warn("Error while communicating with client code for Id:"
						+ getId());
			}
		}
	}

	/**
	 * DO NOT USE. Use of this method will cause undefined behaviour
	 */
	/*
	 * Used internally for MQ polling events. RabbitMqConsumer passes queue
	 * consumer closure events to this method, this in turn notifies the client
	 * code that such an event has taken place.
	 */
	public void mqDisconnectEvent() {
		logger.info("Disconnect event for ID:" + getId());
		isStreaming = false;
		connected = false;
		actionExecuter.execute(new DisconnectedAction(streamingEvents));
		ResourceWorkQueue.removeQueue(getId());
		ResourceWorkerMap.removeResource(getId());
	}

	/**
	 * Stops the monitoring for this resource's MQ queue.
	 */
	@Override
	public void stopStreaming() {
		if (connected == true) {
			logger.debug("Calling MQListener disconnect for resource" + getId());
			MQListener.disconnect(getId());
		}
		connected = false;
		isStreaming = false;
	}

	/**
	 * Pauses the monitoring for this resource's MQ queue. Resume queue
	 * monitoring with unpauseStreaming.
	 */
	@Override
	public void pauseStreaming() {
		isStreaming = false;
	}

	/**
	 * Resumes the monitoring for this resource's MQ queue if it has been
	 * paused.
	 */
	@Override
	public void unpauseStreaming() {
		isStreaming = true;
	}

	/**
	 * Return the resource's ID
	 */
	@Override
	public String getId() {
		return restItem.getContent().getId();
	}

	/**
	 * Return the resource's name
	 */
	@Override
	public String getName() {
		return restItem.getName();
	}

	/**
	 * Return the resource's content
	 */
	@Override
	public Summary getContent() {
		return restItem.getContent();
	}

	// Hook for testing
	protected void setHttpSvcs(HttpServices httpSvcs) {
		ResourceImpl.httpSvcs = httpSvcs;
	}

}
