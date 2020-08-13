/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.model;

import java.io.IOException;
import java.sql.Date;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonLearningGoals {

	private String id;
	private String goal_desc;
	private Date created_on;
	private List<String> goal_content_id;
	private String goal_title;
	private String goal_group;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getGoal_desc() {
		return goal_desc;
	}

	public void setGoal_desc(String goal_desc) {
		this.goal_desc = goal_desc;
	}

	public Date getCreated_on() {
		return created_on;
	}

	public void setCreated_on(Date created_on) {
		this.created_on = created_on;
	}

	public List<String> getGoal_content_id() {
		return goal_content_id;
	}

	public void setGoal_content_id(List<String> goal_content_id) {
		this.goal_content_id = goal_content_id;
	}

	public String getGoal_title() {
		return goal_title;
	}

	public void setGoal_title(String goal_title) {
		this.goal_title = goal_title;
	}

	public String getGoal_group() {
		return goal_group;
	}

	public void setGoal_group(String goal_group) {
		this.goal_group = goal_group;
	}

	public Map<String, Object> toMap(boolean keepNulls) {
		ObjectMapper mapper = new ObjectMapper();
		if (!keepNulls) {
			mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		}
		return mapper.convertValue(this, Map.class);
	}

	public static CommonLearningGoals fromMap(Map<String, Object> map) {
		return new ObjectMapper().convertValue(map, CommonLearningGoals.class);
	}

	public static CommonLearningGoals fromJson(String json) throws IOException {
		return new ObjectMapper().readValue(json, CommonLearningGoals.class);
	}

}
