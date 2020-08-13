/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.repository;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.infosys.model.BatchExecutionData;


@Repository
public interface BatchExecutionRepository extends MongoRepository<BatchExecutionData, ObjectId>{
	
	@Query(value = "{'batch_name': ?0 }")
	public List<BatchExecutionData> findByBatchName(String name, Pageable page);
}
