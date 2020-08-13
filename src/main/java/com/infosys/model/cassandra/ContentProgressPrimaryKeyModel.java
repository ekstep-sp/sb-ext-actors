/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.model.cassandra;

import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.io.Serializable;

@PrimaryKeyClass
public class ContentProgressPrimaryKeyModel implements Serializable{
	private static final long serialVersionUID = 1L;
	
	@PrimaryKeyColumn(name  = "root_org", ordinal = 0,type = PrimaryKeyType.PARTITIONED)
	private String rootOrg;
	
	public String getRootOrg() {
		return rootOrg;
	}

	public void setRootOrg(String rootOrg) {
		this.rootOrg = rootOrg;
	}

	@PrimaryKeyColumn(name = "user_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
	private String userId;
	
	@PrimaryKeyColumn(name = "content_type", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
	private String contentType;
	
	@PrimaryKeyColumn(name = "content_id", ordinal = 2, type = PrimaryKeyType.CLUSTERED)
	private String contentId;

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

    public String getContentId() {
		return contentId;
	}

	public void setContentId(String contentId) {
		this.contentId = contentId;
	}

	

	public ContentProgressPrimaryKeyModel(String rootOrg, String userId, String contentType, String contentId) {
		super();
		this.rootOrg = rootOrg;
		this.userId = userId;
		this.contentType = contentType;
		this.contentId = contentId;
	}

	public ContentProgressPrimaryKeyModel() {
		super();
	}
	
	

}
