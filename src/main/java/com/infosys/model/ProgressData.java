/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;
import java.util.Map;

@Document(collection = "user_content_progress_collection")
public class ProgressData {
	@Id
	private ObjectId id;
	@Field("children_duration")
	private Map<String, Object> childrenDuration;
	@Field("parent_collections")
	private List<String> parentCollections;
	@Field("last_access_res_point")
	private String lastAccessResPoint;
	@Field("resourceType")
	private String contentResourceType;
	@Field("has_assessment")
	private String hasAssessment;
	@Field("thumbnail")
	private String thumbnailUrl;
	@Field("cid")
	private String contentId;
	@Field("last_ts")
	private Long lastTs;
	@Field("contentType")
	private String contentType;
	@Field("uid")
	private String userId;
	@Field("length")
	private Integer duration;
	@Field("source")
	private String source;
	@Field("contentName")
	private String contentName;
	@Field("children_ids")
	private List<String> childrenIds;
	@Field("last_accessed_res_id")
	private String lastAccessedResId;
	@Field("progress")
	private Double progress;
	@Field("num_children")
	private Integer numChildren;
	@Field("assessment_id")
	private String assessmentId;
	@Field("progress_children")
	private Map<String, Object> progressChildren;
	@Field("empMailId")
	private String empMailId;
	@Field("content_id_at_source")
	private String contentIdAtSource;
	@Field("time_spent")
	private Float timeSpent;
	@Field("time_spent_children")
	private Map<String, Object> timeSpentChildren;
	@Field("children_index")
	private Map<String, Object> childrenIndex;
	@Field("first_accessed_on")
	private Long firstAccessedOn;

	public ProgressData(ObjectId id, Map<String, Object> childrenDuration, List<String> parentCollections,
			String lastAccessResPoint, String contentResourceType, String hasAssessment, String thumbnailUrl,
			String contentId, Long lastTs, String contentType, String userId, Integer duration, String source,
			String contentName, List<String> childrenIds, String lastAccessedResId, Double progress,
			Integer numChildren, String assessmentId, Map<String, Object> progressChildren, String empMailId,
			String contentIdAtSource, Float timeSpent, Map<String, Object> timeSpentChildren,
			Map<String, Object> childrenIndex, Long firstAccessedOn) {
		super();
		this.id = id;
		this.childrenDuration = childrenDuration;
		this.parentCollections = parentCollections;
		this.lastAccessResPoint = lastAccessResPoint;
		this.contentResourceType = contentResourceType;
		this.hasAssessment = hasAssessment;
		this.thumbnailUrl = thumbnailUrl;
		this.contentId = contentId;
		this.lastTs = lastTs;
		this.contentType = contentType;
		this.userId = userId;
		this.duration = duration;
		this.source = source;
		this.contentName = contentName;
		this.childrenIds = childrenIds;
		this.lastAccessedResId = lastAccessedResId;
		this.progress = progress;
		this.numChildren = numChildren;
		this.assessmentId = assessmentId;
		this.progressChildren = progressChildren;
		this.empMailId = empMailId;
		this.contentIdAtSource = contentIdAtSource;
		this.timeSpent = timeSpent;
		this.timeSpentChildren = timeSpentChildren;
		this.childrenIndex = childrenIndex;
		this.firstAccessedOn = firstAccessedOn;
	}

	public ObjectId getId() {
		return id;
	}

	public void setId(ObjectId id) {
		this.id = id;
	}

	public Map<String, Object> getChildrenDuration() {
		return childrenDuration;
	}

	public void setChildrenDuration(Map<String, Object> childrenDuration) {
		this.childrenDuration = childrenDuration;
	}

	public List<String> getParentCollections() {
		return parentCollections;
	}

	public void setParentCollections(List<String> parentCollections) {
		this.parentCollections = parentCollections;
	}

	public String getLastAccessResPoint() {
		return lastAccessResPoint;
	}

	public void setLastAccessResPoint(String lastAccessResPoint) {
		this.lastAccessResPoint = lastAccessResPoint;
	}

	public String getContentResourceType() {
		return contentResourceType;
	}

	public void setContentResourceType(String contentResourceType) {
		this.contentResourceType = contentResourceType;
	}

	public boolean getHasAssessment() {
		if(hasAssessment.equals("no"))
			return false;
		else
			return true;
	}

	public void setHasAssessment(boolean hasAssessment) {
		if(hasAssessment)
			this.hasAssessment = "yes";
		else
			this.hasAssessment = "no";
	}

	public String getThumbnailUrl() {
		return thumbnailUrl;
	}

	public void setThumbnailUrl(String thumbnailUrl) {
		this.thumbnailUrl = thumbnailUrl;
	}

	public String getContentId() {
		return contentId;
	}

	public void setContentId(String contentId) {
		this.contentId = contentId;
	}

	public Long getLastTs() {
		return lastTs;
	}

	public void setLastTs(Long lastTs) {
		this.lastTs = lastTs;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public Integer getDuration() {
		return duration;
	}

	public void setDuration(Integer duration) {
		this.duration = duration;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getContentName() {
		return contentName;
	}

	public void setContentName(String contentName) {
		this.contentName = contentName;
	}

	public List<String> getChildrenIds() {
		return childrenIds;
	}

	public void setChildrenIds(List<String> childrenIds) {
		this.childrenIds = childrenIds;
	}

	public String getLastAccessedResId() {
		return lastAccessedResId;
	}

	public void setLastAccessedResId(String lastAccessedResId) {
		this.lastAccessedResId = lastAccessedResId;
	}

	public Double getProgress() {
		return progress;
	}

	public void setProgress(Double progress) {
		this.progress = progress;
	}

	public Integer getNumChildren() {
		return numChildren;
	}

	public void setNumChildren(Integer numChildren) {
		this.numChildren = numChildren;
	}

	public String getAssessmentId() {
		return assessmentId;
	}

	public void setAssessmentId(String assessmentId) {
		this.assessmentId = assessmentId;
	}

	public Map<String, Object> getProgressChildren() {
		return progressChildren;
	}

	public void setProgressChildren(Map<String, Object> progressChildren) {
		this.progressChildren = progressChildren;
	}

	public String getEmpMailId() {
		return empMailId;
	}

	public void setEmpMailId(String empMailId) {
		this.empMailId = empMailId;
	}

	public String getContentIdAtSource() {
		return contentIdAtSource;
	}

	public void setContentIdAtSource(String contentIdAtSource) {
		this.contentIdAtSource = contentIdAtSource;
	}

	public Float getTimeSpent() {
		return timeSpent;
	}

	public void setTimeSpent(Float timeSpent) {
		this.timeSpent = timeSpent;
	}

	public Map<String, Object> getTimeSpentChildren() {
		return timeSpentChildren;
	}

	public void setTimeSpentChildren(Map<String, Object> timeSpentChildren) {
		this.timeSpentChildren = timeSpentChildren;
	}

	public Map<String, Object> getChildrenIndex() {
		return childrenIndex;
	}

	public void setChildrenIndex(Map<String, Object> childrenIndex) {
		this.childrenIndex = childrenIndex;
	}

	public Long getFirstAccessedOn() {
		return firstAccessedOn;
	}

	public void setFirstAccessedOn(Long firstAccessedOn) {
		this.firstAccessedOn = firstAccessedOn;
	}

}
