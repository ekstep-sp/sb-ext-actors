/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.repositoryImpl;

import org.springframework.data.cassandra.core.CassandraBatchOperations;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.repository.query.CassandraEntityInformation;
import org.springframework.data.cassandra.repository.support.SimpleCassandraRepository;
import org.springframework.stereotype.Repository;

import com.infosys.model.cassandra.UserExerciseLastModel;
import com.infosys.model.cassandra.UserExerciseLastPrimaryKeyModel;
import com.infosys.model.cassandra.UserExerciseModel;
import com.infosys.model.cassandra.UserExercisePrimaryKeyModel;
import com.infosys.repository.ExerciseBatchRepository;
import com.infosys.repository.UserExerciseLastRepository;
import com.infosys.repository.UserExerciseRepository;

@Repository
public class ExerciseBatchRepositoryImpl<S>
		extends SimpleCassandraRepository<UserExerciseLastModel, UserExerciseLastPrimaryKeyModel>
		implements ExerciseBatchRepository<S> {

	private final CassandraTemplate operations;

	public ExerciseBatchRepositoryImpl(
			CassandraEntityInformation<UserExerciseLastModel, UserExerciseLastPrimaryKeyModel> metadata,
			CassandraTemplate operations, final UserExerciseRepository userExerciseRepository,
			final UserExerciseLastRepository userExerciseLastRepository) {
		super(metadata, operations);
		this.operations = operations;
	}
	
	@SuppressWarnings("hiding")
	@Override
	  public <S extends UserExerciseLastModel> S insert(final S exercise) {
	    final CassandraBatchOperations batchOps = operations.batchOps();
	    batchOps.insert(exercise);
	    UserExercisePrimaryKeyModel primaryKey= new UserExercisePrimaryKeyModel(exercise.getPrimaryKey().getUserId(), exercise.getPrimaryKey().getContentId(), exercise.getSubmissionId()==null?exercise.getFeedbackSubmissionId():exercise.getSubmissionId());
	    UserExerciseModel primaryExercise= new UserExerciseModel(primaryKey, exercise.getSubmissionTime(), exercise.getResultPercent(), exercise.getSubmissionUrl(), exercise.getSubmissionType(), exercise.getTestcasesFailed(), exercise.getTestcasesPassed(), exercise.getTotalTestcases(), exercise.getFeedbackBy(), exercise.getFeedbackTime(), exercise.getFeedbackUrl(), exercise.getFeedbackType());
	    batchOps.insert(primaryExercise);
	    batchOps.execute();
	    return exercise;
	  }
}
