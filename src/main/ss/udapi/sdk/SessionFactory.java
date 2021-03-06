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

import java.net.URL;

import ss.udapi.sdk.interfaces.Credentials;
import ss.udapi.sdk.interfaces.Session;

/**
 * Factory which supplies session instances to the client.
 * 
 */
public class SessionFactory {

	/**
	 * @param rootURL
	 *            Server end-point as supplied by Sporting Solutions.
	 * @param credentials
	 *            Credentials associated with a valid account grating access to
	 *            the Sporting Solutions Service.
	 * @return A session which provides access to services.
	 */
	public static Session createSession(URL rootURL, Credentials credentials) throws Exception {
		return new SessionImpl(rootURL, credentials);
	}

}
