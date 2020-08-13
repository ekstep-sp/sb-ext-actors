/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.model.cassandra;

import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.io.Serializable;
import java.util.UUID;

@PrimaryKeyClass
public class UserExercisePrimaryKeyModel implements Serializable{
	private static final long serialVersionUID = 1L;
	
	@PrimaryKeyColumn(name = "user_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
	private String userId;
	
	@PrimaryKeyColumn(name = "content_id", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
	private String contentId;
	
	@PrimaryKeyColumn(name = "submission_id", ordinal = 2, type = PrimaryKeyType.CLUSTERED)
	private UUID submissionId;

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getContentId() {
		return contentId;
	}

	public void setContentId(String contentId) {
		this.contentId = contentId;
	}

	public UUID getSubmissionId() {
		return submissionId;
	}

	public void setSubmissionId(UUID submissionId) {
		this.submissionId = submissionId;
	}

	public UserExercisePrimaryKeyModel(String userId, String contentId, UUID submissionId) {
		super();
		this.userId = userId;
		this.contentId = contentId;
		this.submissionId = submissionId;
	}

	public UserExercisePrimaryKeyModel() {
		super();
	}
}
