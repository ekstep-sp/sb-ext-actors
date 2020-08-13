/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.model.cassandra;

import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.io.Serializable;

@PrimaryKeyClass
public class GroupUserPrimaryKeyModel implements Serializable{
	private static final long serialVersionUID = 1L;
	
	@PrimaryKeyColumn(name = "group_id", ordinal = 1, type = PrimaryKeyType.PARTITIONED)
	private String groupId;
	
	@PrimaryKeyColumn(name = "user_id", ordinal = 0, type = PrimaryKeyType.CLUSTERED)
	private String userId;

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}
	
	public GroupUserPrimaryKeyModel(String groupId, String userId) {
		super();
		this.groupId = groupId;
		this.userId = userId;
	}

	public GroupUserPrimaryKeyModel() {
		super();
	}

	
}
