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

package ss.udapi.sdk.interfaces;

import ss.udapi.sdk.model.Summary;
import ss.udapi.sdk.streaming.Event;

import java.util.List;

/**
 * Please see implementing classes
 */
public interface Resource {

	public String getId();

	public String getName();

	public Summary getContent();

	public String getSnapshot() throws Exception;

	public void startStreaming(List<Event> streamingEvents) throws Exception;

	public void stopStreaming();

	public void pauseStreaming();

	public void unpauseStreaming();

}
