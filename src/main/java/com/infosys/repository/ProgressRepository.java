/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.repository;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.infosys.model.ProgressData;


@Repository
public interface ProgressRepository extends MongoRepository<ProgressData, ObjectId>,ProgressRepositoryCustom {

	public ProgressData findByContentIdAndEmpMailId(String cid, String email_id);
	
	@Query(value = "{ 'contentType' : 'Course', 'empMailId' : ?0, 'progress':1 }", fields = "{ 'cid' : 1,'_id':0 }")
    public List<ProgressData> findCompletedCourses(String email);
	
	@Query(value = "{ 'resourceType' : 'Quiz', 'empMailId' : ?0, 'progress':1 }", fields = "{ 'cid' : 1,'_id':0 }")
    public List<ProgressData> findCompletedQuizzes(String email);
	
	@Query(value = "{ 'empMailId' : ?0, 'cid': { $in : ?1 }}", fields = "{ 'cid' : 1, 'progress': 1, '_id':0 }")
    public List<ProgressData> findProgressOfCourses(String email, List<String> ids);

	@Query(value = "{ 'empMailId' : ?0, 'cid': ?1 }", fields = "{ 'first_accessed_on' : 1}")
    public List<ProgressData> findFirstAccessedOn(String email, String ids);

	
}
