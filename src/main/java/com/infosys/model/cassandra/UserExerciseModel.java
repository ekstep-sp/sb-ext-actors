/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.model.cassandra;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.Date;

@Table("user_exercise")
public class UserExerciseModel {

	@PrimaryKey
	private UserExercisePrimaryKeyModel primaryKey;
	
	@Column("submission_time")
	private Date submissionTime;
	
	@Column("result_percent")
	private Float resultPercent;
	
	@Column("submission_url")
	private String submissionUrl;
	
	@Column("submission_type")
	private String submissionType;
	
	@Column("testcases_failed")
	private int testcasesFailed;
	
	@Column("testcases_passed")
	private int testcasesPassed;
	
	@Column("total_testcases")
	private int totalTestcases;
	
	@Column("feedback_by")
	private String feedbackBy;
	
	@Column("feedback_time")
	private Date feedbackTime;
	
	@Column("feedback_url")
	private String feedbackUrl;
	
	@Column("feedback_type")
	private String feedbackType;

	public UserExercisePrimaryKeyModel getPrimaryKey() {
		return primaryKey;
	}

	public void setPrimaryKey(UserExercisePrimaryKeyModel primaryKey) {
		this.primaryKey = primaryKey;
	}

	public Date getSubmissionTime() {
		return submissionTime;
	}

	public void setSubmissionTime(Date submissionTime) {
		this.submissionTime = submissionTime;
	}

	public Float getResultPercent() {
		return resultPercent;
	}

	public void setResultPercent(Float resultPercent) {
		this.resultPercent = resultPercent;
	}

	public String getSubmissionUrl() {
		return submissionUrl;
	}

	public void setSubmissionUrl(String submissionUrl) {
		this.submissionUrl = submissionUrl;
	}

	public String getSubmissionType() {
		return submissionType;
	}

	public void setSubmissionType(String submissionType) {
		this.submissionType = submissionType;
	}

	public int getTestcasesFailed() {
		return testcasesFailed;
	}

	public void setTestcasesFailed(int testcasesFailed) {
		this.testcasesFailed = testcasesFailed;
	}

	public int getTestcasesPassed() {
		return testcasesPassed;
	}

	public void setTestcasesPassed(int testcasesPassed) {
		this.testcasesPassed = testcasesPassed;
	}

	public int getTotalTestcases() {
		return totalTestcases;
	}

	public void setTotalTestcases(int totalTestcases) {
		this.totalTestcases = totalTestcases;
	}

	public String getFeedbackBy() {
		return feedbackBy;
	}

	public void setFeedbackBy(String feedbackBy) {
		this.feedbackBy = feedbackBy;
	}

	public Date getFeedbackTime() {
		return feedbackTime;
	}

	public void setFeedbackTime(Date feedbackTime) {
		this.feedbackTime = feedbackTime;
	}

	public String getFeedbackUrl() {
		return feedbackUrl;
	}

	public void setFeedbackUrl(String feedbackUrl) {
		this.feedbackUrl = feedbackUrl;
	}

	public String getFeedbackType() {
		return feedbackType;
	}

	public void setFeedbackType(String feedbackType) {
		this.feedbackType = feedbackType;
	}

	public UserExerciseModel(UserExercisePrimaryKeyModel primaryKey, Date submissionTime, Float resultPercent,
			String submissionUrl, String submissionType, int testcasesFailed, int testcasesPassed, int totalTestcases,
			String feedbackBy, Date feedbackTime, String feedbackUrl, String feedbackType) {
		super();
		this.primaryKey = primaryKey;
		this.submissionTime = submissionTime;
		this.resultPercent = resultPercent;
		this.submissionUrl = submissionUrl;
		this.submissionType = submissionType;
		this.testcasesFailed = testcasesFailed;
		this.testcasesPassed = testcasesPassed;
		this.totalTestcases = totalTestcases;
		this.feedbackBy = feedbackBy;
		this.feedbackTime = feedbackTime;
		this.feedbackUrl = feedbackUrl;
		this.feedbackType = feedbackType;
	}

	public UserExerciseModel() {
		super();
	}
	
}
