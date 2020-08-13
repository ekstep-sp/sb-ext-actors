/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.model.cassandra;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.Date;

@Table("group_user_mapping")
public class GroupUserModel {

	@PrimaryKey
	private GroupUserPrimaryKeyModel primaryKey;
	
	@Column("group_name")
	private String groupName;
	
	@Column("date_created")
	private Date dateCreated;

	public GroupUserPrimaryKeyModel getPrimaryKey() {
		return primaryKey;
	}

	public void setPrimaryKey(GroupUserPrimaryKeyModel primaryKey) {
		this.primaryKey = primaryKey;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public Date getDateCreated() {
		return dateCreated;
	}

	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}

	public GroupUserModel(GroupUserPrimaryKeyModel primaryKey, String groupName, Date dateCreated) {
		super();
		this.primaryKey = primaryKey;
		this.groupName = groupName;
		this.dateCreated = dateCreated;
	}

	public GroupUserModel() {
		super();
	}
	
	
}
