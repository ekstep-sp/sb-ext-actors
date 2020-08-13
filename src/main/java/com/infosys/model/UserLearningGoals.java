/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.sql.Timestamp;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserLearningGoals {
	private float goal_progress;
    private String goal_id;
    private List<String> goal_content_id;
    @JsonIgnore
    private String user_id;
    private Timestamp last_updated_on;
    private Timestamp created_on;
    private String goal_desc;
    private String goal_type;
    private String goal_title;
    private int goal_duration;
    private String goal_start_date;
    private String goal_end_date;
    private List<UserGoalProgress> resource_progress;
    private String shared_by;
    private float total_goal_duration;
    private float total_read_duration;

	public float getGoalProgress()
	{
		return goal_progress;
	}

    public void setGoalProgress(float goalProgress)
	{
		this.goal_progress=goalProgress;
	}

    public String getGoal_id() {
		return goal_id;
	}

    public void setGoal_id(String goal_id) {
		this.goal_id = goal_id;
	}

    public List<String> getGoal_content_id() {
		return goal_content_id;
	}

    public void setGoal_content_id(List<String> goal_content_id) {
		this.goal_content_id = goal_content_id;
	}

    public String getUser_id() {
		return user_id;
	}

    public void setUser_id(String user_id) {
		this.user_id = user_id;
	}

    public Timestamp getLast_updated_on() {
		return last_updated_on;
	}

    public void setLast_updated_on(Timestamp last_updated_on) {
		this.last_updated_on = last_updated_on;
	}

    public Timestamp getCreated_on() {
		return created_on;
	}

    public void setCreated_on(Timestamp created_on) {
		this.created_on = created_on;
	}

    public String getGoal_desc() {
		return goal_desc;
	}

    public void setGoal_desc(String goal_desc) {
		this.goal_desc = goal_desc;
	}

    public String getGoal_type() {
		return goal_type;
	}

    public void setGoal_type(String goal_type) {
		this.goal_type = goal_type;
	}

    public String getGoal_title() {
		return goal_title;
	}

    public void setGoal_title(String goal_title) {
		this.goal_title = goal_title;
	}

    public List<UserGoalProgress> getResource_progress() {
		return resource_progress;
	}

    public void setResource_progress(List<UserGoalProgress> resource_progress) {
		this.resource_progress = resource_progress;
	}

    public int getGoal_duration() {
		return goal_duration;
	}

    public void setGoal_duration(int goal_duration) {
		this.goal_duration = goal_duration;
	}

    public String getGoal_start_date() {
		return goal_start_date;
	}

    public void setGoal_start_date(String goal_start_date) {
		this.goal_start_date = goal_start_date;
	}

    public String getGoal_end_date() {
		return goal_end_date;
	}

    public void setGoal_end_date(String goal_end_date) {
		this.goal_end_date = goal_end_date;
	}

    public String getShared_by() {
		return shared_by;
	}

    public void setShared_by(String shared_by) {
		this.shared_by = shared_by;
	}

    public float getTotal_goal_duration() {
		return total_goal_duration;
	}

    public void setTotal_goal_duration(float total_goal_duration) {
		this.total_goal_duration = total_goal_duration;
	}

    public float getTotal_read_duration() {
		return total_read_duration;
	}

    public void setTotal_read_duration(float total_read_duration) {
		this.total_read_duration = total_read_duration;
	}
}



