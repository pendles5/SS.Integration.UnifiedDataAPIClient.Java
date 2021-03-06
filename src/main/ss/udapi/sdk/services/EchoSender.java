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

package ss.udapi.sdk.services;

import java.net.URI;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import ss.udapi.sdk.services.JsonHelper;
import ss.udapi.sdk.model.ServiceRequest;
import ss.udapi.sdk.model.StreamEcho;

/*
 * Uses EchoResourceMap to manage the count of echo failures for each resource/fixture.
 */
public class EchoSender implements Runnable {
	
	private static Logger logger = Logger.getLogger(EchoSender.class);
	private static EchoSender instance = null;
	private static HttpServices httpSvcs = new HttpServices();
	private URI amqpURI;
	private ServiceRequest resources = new ServiceRequest();
	private static final String THREAD_NAME = "Echo_Thread";
	private static ReentrantLock echoSenderLock = new ReentrantLock();
	private static boolean terminate = false;

	private EchoSender(String amqpDest, ServiceRequest resources) {
		this.resources = resources;
		try {
			this.amqpURI = new URI(amqpDest);
		} catch (Exception ex) {
			logger.debug(ex);
		}
	}

	public static EchoSender getEchoSender(String amqpDest, ServiceRequest resources) {
		try {
			echoSenderLock.lock();
			logger.debug("Retrieving EchoSender or create it if it doesn't exist");
			if (instance == null) {
				instance = new EchoSender(amqpDest, resources);
			}
		} catch (Exception ex) {
			logger.error("Could not initialiaze EchoSender Listener.");
			throw new MissingResourceException(
					"Service threadpool has become corrupted",
					"ss.udapi.sdk.services.ActionThreadExecutor", "EchoSender");
		} finally {
			echoSenderLock.unlock();
		}
		return instance;
	}

	@Override
	public void run() {
		
		terminate = false;
		logger.info("Starting echos.");
		EchoResourceMap echoMap = EchoResourceMap.getEchoMap();

		// Get the connection details for the MQ box.
		String path = amqpURI.getRawPath();
		String queue = path.substring(path.indexOf('/', 1) + 1);
		String virtualHost = uriDecode(amqpURI.getPath().substring(1, path.indexOf('/', 1)));

		// Prepare a simple message to send to the echo system. This message
		// will come back in each resource's MQ queue.
		StreamEcho streamEcho = new StreamEcho();
		streamEcho.setHost(virtualHost);
		streamEcho.setQueue(queue);
		String guid = UUID.randomUUID().toString();
		DateFormat df = new SimpleDateFormat("yyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
		streamEcho.setMessage(guid + ";" + df.format(new Date()));
		String stringStreamEcho = JsonHelper.ToJson(streamEcho);
		Thread.currentThread().setName(THREAD_NAME);

		while (true) {
			try {

				// Send the message to the Sporting Solution's end-point.
				httpSvcs.processRequest(
						resources,
						"http://api.sportingsolutions.com/rels/stream/batchecho",
						resources.getServiceRestItems().get(0).getName(),
						stringStreamEcho);
				
				logger.info("Batch echo sent: " + stringStreamEcho);

				// After the message is sent increase the number of echos sent
				// for all resources.
				// The number of missed echos is configured in:
				// conf/sdk.properties using "ss.echo_max_missed_echos"
				Set<String> defaulters = echoMap.incrAll(Integer
						.parseInt(SystemProperties.get("ss.echo_max_missed_echos")));
				
				Iterator<String> keyIter = defaulters.iterator();

				/*
				 * EchoMap returns a list of resources which appear to have
				 * unresponsive queues (the number of echo retries has been
				 * exceeded.
				 * 
				 * At this point we disconnect the queue consumer from the MQ
				 * service. This triggers an action in RabbitMQ which alerts the
				 * resource/fixture of the failure. The resource is then
				 * responsible for communicating the problem to the client code.
				 */
				while (keyIter.hasNext()) {
					String resourceId = keyIter.next();
					logger.warn("Attempting to disconnect resource: "
							+ resourceId
							+ " due maximum number of echo retries reached");
					MQListener.disconnect(resourceId);
				}

				// The interval between echos is configured in:
				// conf/sdk.properties using "ss.echo_sender_interval"
				Thread.sleep(Integer.parseInt(SystemProperties.get("ss.echo_sender_interval")) * 1000);

				if (terminate == true) {
					return;
				}
				
			} catch (Exception ex) {
				logger.error("An error occured: " + ex);
			}
		}
	}

	/*
	 * Tidy up the path to something usable
	 */
	private String uriDecode(String s) {
		try {
			// URLDecode decodes '+' to a space, as for form encoding. So
			// protect plus signs.
			return URLDecoder.decode(s.replace("+", "%2B"), "US-ASCII");
		} catch (java.io.UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	// for unit testing
	public static void terminate() {
		terminate = true;
	}

}
