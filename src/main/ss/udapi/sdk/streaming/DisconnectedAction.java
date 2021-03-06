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

package ss.udapi.sdk.streaming;

import java.util.List;

import org.apache.log4j.Logger;

/**
 * Please see superclass.
 */
public class DisconnectedAction extends Action {

	private static Logger logger = Logger.getLogger(DisconnectedAction.class.getName());

	/**
	 * We're listening for MQ Disconnect events.
	 * 
	 * @param events
	 *            List of events the client code wants to be informed about.
	 */
	public DisconnectedAction(List<Event> events) {
		super(events, DisconnectedEvent.class);
	}

	/**
	 * Notifications are executed in their own thread.
	 */
	@Override
	public void run() {
		try {
			execute("Disconnected Action");
		} catch (Exception ex) {
			logger.warn("Error", ex);
		}
	}
}
