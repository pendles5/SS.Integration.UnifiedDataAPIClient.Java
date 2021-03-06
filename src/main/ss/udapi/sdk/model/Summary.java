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

package ss.udapi.sdk.model;

import java.util.List;

/**
 * Model used for JSON mapping - Direct use of this class will lead to undefined
 * behaviour.
 */
public class Summary {
	
	private String Id;
	private String Date;
	private String StartTime;
	private int Sequence;
	private List<Tag> Tags;
	private int MatchStatus;

	public String getId() {
		return Id;
	}

	public void setId(String id) {
		Id = id;
	}

	public String getDate() {
		return Date;
	}

	public void setDate(String date) {
		Date = date;
	}

	public String getStartTime() {
		return StartTime;
	}

	public void setStartTime(String startTime) {
		StartTime = startTime;
	}

	public int getSequence() {
		return Sequence;
	}

	public void setSequence(int sequence) {
		Sequence = sequence;
	}

	public List<Tag> getTags() {
		return Tags;
	}

	public void setTags(List<Tag> tags) {
		Tags = tags;
	}

	public int getMatchStatus() {
		return MatchStatus;
	}

	public void setMatchStatus(int matchStatus) {
		MatchStatus = matchStatus;
	}

}
