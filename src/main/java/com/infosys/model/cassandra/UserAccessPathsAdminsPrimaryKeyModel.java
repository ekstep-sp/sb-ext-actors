/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.model.cassandra;

import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

@Table("user_access_paths_admins")
public class UserAccessPathsAdminsPrimaryKeyModel implements Serializable {
	private static final long serialVersionUID = 1L;

	@PrimaryKeyColumn(name = "admin_id", ordinal = 2, type = PrimaryKeyType.PARTITIONED)
	private UUID adminId;

	@PrimaryKeyColumn(ordinal = 0, type = PrimaryKeyType.PARTITIONED, name = "root_org")
	private String rootOrg;

	@PrimaryKeyColumn(name = "org", ordinal = 1, type = PrimaryKeyType.PARTITIONED)
	private String org;

	@Column
	private Set<String> roles;

	public UUID getAdminId() {
		return adminId;
	}

	public void setAdminId(UUID adminId) {
		this.adminId = adminId;
	}

	public String getRootOrg() {
		return rootOrg;
	}

	public void setRootOrg(String rootOrg) {
		this.rootOrg = rootOrg;
	}

	public Set<String> getRoles() {
		return roles;
	}

	public void setRoles(Set<String> roles) {
		this.roles = roles;
	}

	public String getOrg() {
		return org;
	}

	public void setOrg(String org) {
		this.org = org;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public UserAccessPathsAdminsPrimaryKeyModel(UUID adminId, String rootOrg, String org, Set<String> roles) {
		super();
		this.adminId = adminId;
		this.rootOrg = rootOrg;
		this.org = org;
		this.roles = roles;
	}

	public UserAccessPathsAdminsPrimaryKeyModel() {
		super();
	}
}
