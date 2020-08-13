/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.model.cassandra;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.Date;
import java.util.UUID;

@Table("user_exercise_last_by_feedback")
public class UserExerciseLastByFeedbackModel {

    @PrimaryKey
    private UserExerciseLastByFeedbackPrimaryKeyModel primaryKey;

    @Column("submission_id")
    private UUID submissionId;

    @Column("feedback_by")
    private String feedbackBy;

    @Column("feedback_time")
    private Date feedbackTime;

    @Column("feedback_submission_id")
    private UUID feedbackSubmissionId;

    public UserExerciseLastByFeedbackModel(UserExerciseLastByFeedbackPrimaryKeyModel primaryKey, UUID submissionId,
                                           String feedbackBy, Date feedbackTime, UUID feedbackSubmissionId) {
        super();
        this.primaryKey = primaryKey;
        this.submissionId = submissionId;
        this.feedbackBy = feedbackBy;
        this.feedbackTime = feedbackTime;
        this.feedbackSubmissionId = feedbackSubmissionId;
    }

    public UserExerciseLastByFeedbackModel() {
        super();
    }

    public UserExerciseLastByFeedbackPrimaryKeyModel getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(UserExerciseLastByFeedbackPrimaryKeyModel primaryKey) {
        this.primaryKey = primaryKey;
    }

    public UUID getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(UUID submissionId) {
        this.submissionId = submissionId;
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

    public UUID getFeedbackSubmissionId() {
        return feedbackSubmissionId;
    }

    public void setFeedbackSubmissionId(UUID feedbackSubmissionId) {
        this.feedbackSubmissionId = feedbackSubmissionId;
    }
}
