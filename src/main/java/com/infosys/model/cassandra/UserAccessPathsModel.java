/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.model.cassandra;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.Set;

@Table("user_access_paths")
public class UserAccessPathsModel {

	@PrimaryKey
	private UserAccessPathsPrimaryKeyModel primaryKey;
	
	@Column("access_paths")
	private Set<String> accessPaths;
	
	@Column("temporary")
	private Boolean temporary;
	
	@Column("ttl")
	private Integer ttl;

	public UserAccessPathsPrimaryKeyModel getPrimaryKey() {
		return primaryKey;
	}

	public void setPrimaryKey(UserAccessPathsPrimaryKeyModel primaryKey) {
		this.primaryKey = primaryKey;
	}

	public Set<String> getAccessPaths() {
		return accessPaths;
	}

	public void setAccessPaths(Set<String> accessPaths) {
		this.accessPaths = accessPaths;
	}

	public Boolean getTemporary() {
		return temporary;
	}

	public void setTemporary(Boolean temporary) {
		this.temporary = temporary;
	}

	public UserAccessPathsModel(UserAccessPathsPrimaryKeyModel primaryKey, Set<String> accessPaths, Boolean temporary, Integer ttl) {
		super();
		this.primaryKey = primaryKey;
		this.accessPaths = accessPaths;
		this.temporary = temporary;
		this.ttl = ttl;
	}

	public UserAccessPathsModel() {
		super();
	}

	public Integer getTtl() {
		return ttl;
	}

	public void setTtl(Integer ttl) {
		this.ttl = ttl;
	}
}
