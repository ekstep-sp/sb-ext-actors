/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
/**
© 2017 - 2019 Infosys Limited, Bangalore, India. All Rights Reserved. 
Version: 1.10

Except for any free or open source software components embedded in this Infosys proprietary software program (“Program”),
this Program is protected by copyright laws, international treaties and other pending or existing intellectual property rights in India,
the United States and other countries. Except as expressly permitted, any unauthorized reproduction, storage, transmission in any form or
by any means (including without limitation electronic, mechanical, printing, photocopying, recording or otherwise), or any distribution of 
this Program, or any portion of it, may result in severe civil and criminal penalties, and will be prosecuted to the maximum extent possible
under the law.

Highly Confidential
 
*/

package com.infosys.model.cassandra;

import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.io.Serializable;
import java.util.Date;

@PrimaryKeyClass
public class InfyMeDownloadDataByDatePrimaryKeyModel implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5043013648189312002L;

	@PrimaryKeyColumn(name = "bucket", ordinal = 0, type = PrimaryKeyType.CLUSTERED)
	private String bucket;

	@PrimaryKeyColumn(name = "download_date", ordinal = 1, ordering = Ordering.DESCENDING, type = PrimaryKeyType.PARTITIONED)
	private Date downloadDate;

	@PrimaryKeyColumn(name = "user_email", ordinal = 2, ordering = Ordering.ASCENDING, type = PrimaryKeyType.CLUSTERED)
	private String email;

	public InfyMeDownloadDataByDatePrimaryKeyModel(String bucket, Date downloadDate, String email) {
		super();
		this.bucket = bucket;
		this.downloadDate = downloadDate;
		this.email = email;
	}

	public InfyMeDownloadDataByDatePrimaryKeyModel() {
		super();
	}

	@Override
	public String toString() {
		return "InfyMeDownloadDataByDatePrimaryKeyModel [bucket=" + bucket + ", downloadDate=" + downloadDate
				+ ", email=" + email + "]";
	}

}