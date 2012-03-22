//Copyright 2012 Spin Services Limited

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

public class Participant {
	
	public void setName(String name) {
		this.Name = name;
	}
	public String getName() {
		return Name;
	}
	
	public void setId(Integer id) {
		this.Id = id;
	}
	public Integer getId() {
		return Id;
	}
	
	private String Name;
	private Integer Id;
}