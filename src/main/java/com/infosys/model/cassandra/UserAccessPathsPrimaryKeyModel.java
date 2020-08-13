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
public class UserAccessPathsPrimaryKeyModel implements Serializable{
	private static final long serialVersionUID = 1L;
	
	@PrimaryKeyColumn(name = "root_org", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
	private String rootOrg;
	
	@PrimaryKeyColumn(name = "org", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
	private String org;
	
	@PrimaryKeyColumn(name = "user_id", ordinal = 0, type = PrimaryKeyType.CLUSTERED)
	private UUID userId;
	
	@PrimaryKeyColumn(name = "cas_id", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
	private UUID casId;

	public String getRootOrg() {
		return rootOrg;
	}

	public void setRootOrg(String rootOrg) {
		this.rootOrg = rootOrg;
	}

	public String getOrg() {
		return org;
	}

	public void setOrg(String org) {
		this.org = org;
	}

	public UUID getUserId() {
		return userId;
	}

	public void setUserId(UUID userId) {
		this.userId = userId;
	}

	public UUID getCasId() {
		return casId;
	}

	public void setCasId(UUID casId) {
		this.casId = casId;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public UserAccessPathsPrimaryKeyModel(String rootOrg, String org, UUID userId, UUID casId) {
		super();
		this.rootOrg = rootOrg;
		this.org = org;
		this.userId = userId;
		this.casId = casId;
	}

	public UserAccessPathsPrimaryKeyModel() {
		super();
	}
}
