/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "daily_time_spent_collection")
public class TimeSpentData {
	@Id
	private ObjectId id;
	@Field("uid")
	private String uid;
	@Field("date")
	private String date;
	@Field("timespent")
	private Float timeSpent;
	@Field("empMailId")
	private String empMailId;

	public ObjectId getId() {
		return id;
	}

	public void setId(ObjectId id) {
		this.id = id;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public Float getTimeSpent() {
		return timeSpent;
	}

	public void setTimeSpent(Float timeSpent) {
		this.timeSpent = timeSpent;
	}

	public String getEmpMailId() {
		return empMailId;
	}

	public void setEmpMailId(String empMailId) {
		this.empMailId = empMailId;
	}

	public TimeSpentData(ObjectId id, String uid, String date, Float timeSpent, String empMailId) {
		super();
		this.id = id;
		this.uid = uid;
		this.date = date;
		this.timeSpent = timeSpent;
		this.empMailId = empMailId;
	}

	public TimeSpentData() {
	}

}
