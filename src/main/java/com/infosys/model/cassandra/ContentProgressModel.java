/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.model.cassandra;

import org.springframework.data.annotation.Transient;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;


@Table("user_content_progress")
public class ContentProgressModel {

	@PrimaryKey
	private ContentProgressPrimaryKeyModel primaryKey;

	@Column("progress")
	private Float progress;

	@Column("last_TS")
	private Date lastTS;
	
	@Column("first_completed_on")
	private Date firstCompletedOn;
	
	@Column("first_accessed_on")
	private Date firstAccessedOn;

	@Column("updated_by")
	private String updatedBy;
	
	@Column("visited_set")
	private Set<String> visitedSet;
	
	@Transient
	private List<String> parentList;

	@Transient
	private List<String> childrenList;

	public ContentProgressPrimaryKeyModel getPrimaryKey() {
		return primaryKey;
	}

	public void setPrimaryKey(ContentProgressPrimaryKeyModel primaryKey) {
		this.primaryKey = primaryKey;
	}

	public ContentProgressModel(ContentProgressPrimaryKeyModel primaryKey, Float progress, Date lastTS,
								Date firstCompletedOn, Date firstAccessedOn, Set<String> visitedSet,
								List<String> parentList, List<String> childrenList) {
		super();
		this.primaryKey = primaryKey;
		this.progress = progress;
		this.lastTS = lastTS;
		this.firstCompletedOn = firstCompletedOn;
		this.firstAccessedOn = firstAccessedOn;
		this.updatedBy = "api";
		this.visitedSet = visitedSet;
		this.parentList = parentList;
		this.childrenList = childrenList;
	}

	public ContentProgressModel(ContentProgressPrimaryKeyModel primaryKey) {
		super();
		this.primaryKey = primaryKey;
		this.updatedBy = "api";
		this.childrenList = new ArrayList<>();
		this.parentList = new ArrayList<>();
	}

	public Date getLastTS() {
		return lastTS;
	}

	public void setLastTS(Date lastTS) {
		this.lastTS = lastTS;
	}

	public Date getFirstCompletedOn() {
		return firstCompletedOn;
	}

	public void setFirstCompletedOn(Date firstCompletedOn) {
		this.firstCompletedOn = firstCompletedOn;
	}

	public Date getFirstAccessedOn() {
		return firstAccessedOn;
	}

	public void setFirstAccessedOn(Date firstAccessedOn) {
		this.firstAccessedOn = firstAccessedOn;
	}

	public ContentProgressModel() {
		super();
		this.updatedBy = "api";
	}

	public Float getProgress() {
		return progress;
	}
	
	public Set<String> getVisitedSet() {
		return visitedSet;
	}

	public void setVisitedSet(Set<String> visitedSet) {
		this.visitedSet = visitedSet;
	}

	public List<String> getParentList() {
		return parentList;
	}

	public void setParentList(List<String> parentList) {
		this.parentList = parentList;
	}

	public List<String> getChildrenList() {
		return childrenList;
	}

	public void setChildrenList(List<String> childrenList) {
		this.childrenList = childrenList;
	}

	public void setProgress(Float progress) {
		this.progress = progress;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}
	
	

}
