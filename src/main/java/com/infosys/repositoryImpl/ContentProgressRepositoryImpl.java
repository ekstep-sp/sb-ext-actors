/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.repositoryImpl;


import com.infosys.model.cassandra.ContentProgressModel;
import com.infosys.model.cassandra.UserHistoryModel;
import com.infosys.repository.ContentProgressRepositoryCustom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.cassandra.core.CassandraBatchOperations;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.stereotype.Repository;

@Repository
public class ContentProgressRepositoryImpl implements ContentProgressRepositoryCustom {

	@Autowired
	CassandraOperations cassandraOperations;
	
	@Override
	public Iterable<ContentProgressModel> updateProgress(Iterable<ContentProgressModel> content, Iterable<UserHistoryModel> contentHistory) {
		CassandraBatchOperations batchOps = cassandraOperations.batchOps();
		batchOps.insert(content);
		batchOps.insert(contentHistory);
		batchOps.execute();
		return content;
	}

}
