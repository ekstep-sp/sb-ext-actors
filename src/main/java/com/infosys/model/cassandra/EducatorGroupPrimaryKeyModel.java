/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.model.cassandra;

import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.io.Serializable;

@PrimaryKeyClass
public class EducatorGroupPrimaryKeyModel implements Serializable{
	private static final long serialVersionUID = 1L;
	
	@PrimaryKeyColumn(name = "educator_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
	private String educatorId;
	
	@PrimaryKeyColumn(name = "group_id", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
	private String groupId;

	public String getEducatorId() {
		return educatorId;
	}

	public void setEducatorId(String educatorId) {
		this.educatorId = educatorId;
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}
	
	public EducatorGroupPrimaryKeyModel(String educatorId, String groupId) {
		super();
		this.educatorId = educatorId;
		this.groupId = groupId;
	}

	public EducatorGroupPrimaryKeyModel() {
		super();
	}
}
