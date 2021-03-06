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

package ss.udapi.sdk.examples.model;

import java.util.HashMap;
import java.util.Map;

public class Selection {

	public Selection() {
		this.Tags = new HashMap<String, Object>();
	}

	public String getId() {
		return Id;
	}

	public void setId(String id) {
		this.Id = id;
	}

	public String getName() {
		return Name;
	}

	public void setName(String name) {
		this.Name = name;
	}

	public Map<String, Object> getTags() {
		return Tags;
	}

	public void setTags(Map<String, Object> tags) {
		this.Tags = tags;
	}

	public Double getPrice() {
		return Price;
	}

	public void setPrice(Double price) {
		this.Price = price;
	}

	public String getStatus() {
		return Status;
	}

	public void setStatus(String status) {
		this.Status = status;
	}

	public Boolean getTradable() {
		return Tradable;
	}

	public void setTradable(Boolean tradable) {
		this.Tradable = tradable;
	}

	private String Id;
	private String Name;
	private Map<String, Object> Tags;
	private Double Price;
	private String Status;
	private Boolean Tradable;
}
