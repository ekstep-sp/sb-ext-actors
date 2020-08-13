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

package com.infosys.repository;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.infosys.model.cassandra.InfyMeDownloadDataByDateModel;
import com.infosys.model.cassandra.InfyMeDownloadDataByDatePrimaryKeyModel;

@Repository
public interface InfyMeDownloadDataByDateRepository extends CassandraRepository<InfyMeDownloadDataByDateModel,InfyMeDownloadDataByDatePrimaryKeyModel>{
	
	@Query(value = "select bucket, download_date , count(*) as downloads from infy_me_download_data_by_date group by bucket,download_date")
	public List<Map<String,Object>> getDownloadsByDate();
	
	@Query(value = "select bucket, download_date , count(*) as downloads from infy_me_download_data_by_date "
			+ " where bucket = 'common_bucket' and download_date <= :endDate and download_date >= :startDate group by bucket,download_date")
	public List<Map<String,Object>> getDownloadsByDateBetween(@Param("startDate") Date startDate,@Param("endDate") Date endDate);
	
	@Query(value = "select  bucket,count(*) as downloads from infy_me_download_data_by_date  where bucket ='common_bucket' and download_date = :date")
	public Map<String,Object> getDownloadsOn(@Param("date") Date date);
}
