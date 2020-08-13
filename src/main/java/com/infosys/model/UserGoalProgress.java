/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserGoalProgress {

	private String resource_id;
	private float time_spent;
	private float time_left;
	private float total_duration;
	private float pending;
	private String resource_name;
	private String resource_desc;
	private float resource_progress;
	private String mimeType;
	private String contentType;

	public String getResource_id() {
		return resource_id;
	}

	public void setResource_id(String resource_id) {
		this.resource_id = resource_id;
	}

	public float getTime_spent() {
		return time_spent;
	}

	public void setTime_spent(float time_spent) {
		this.time_spent = time_spent;
	}

	public float getTime_left() {
		return time_left;
	}

	public void setTime_left(float time_left) {
		this.time_left = time_left;
	}

	public String getResource_name() {
		return resource_name;
	}

	public void setResource_name(String resource_name) {
		this.resource_name = resource_name;
	}

	public String getResource_desc() {
		return resource_desc;
	}

	public void setResource_desc(String resource_desc) {
		this.resource_desc = resource_desc;
	}

	public float getResource_progress() {
		return resource_progress;
	}

	public void setResource_progress(float resource_progress) {
		this.resource_progress = resource_progress;
	}

	public float getTotal_duration() {
		return total_duration;
	}

	public void setTotal_duration(float total_duration) {
		this.total_duration = total_duration;
	}

	public float getPending() {
		return pending;
	}

	public void setPending(float pending) {
		this.pending = pending;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
}
