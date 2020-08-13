/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.repositoryImpl;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;

import com.infosys.repository.TimeSpentRepositoryCustom;
import com.infosys.util.LexJsonKey;

@Repository
public class TimeSpentRepositoryImpl implements TimeSpentRepositoryCustom {

	private final MongoTemplate mongotemplate;

	@Autowired
	public TimeSpentRepositoryImpl(MongoTemplate mongotemplate) {
		this.mongotemplate = mongotemplate;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Map<String, Object> getUserDurationStats(String emailId, Date startDate, Date endDate) {
		Map<String, Object> ret = new HashMap<>();
		try {
			MatchOperation matchStage = Aggregation.match(Criteria.where("empMailId").is(emailId).and("date")
					.gte(LexJsonKey.formatter.format(startDate)).lte(LexJsonKey.formatter.format(endDate)));
			GroupOperation groupStage = Aggregation.group("date").sum("timespent").as("duration");
			ProjectionOperation projectStage = Aggregation.project("duration").andExpression("_id").as("day")
					.andExclude("_id");

			Aggregation aggregation = Aggregation.newAggregation(matchStage, groupStage, projectStage);

			// aggregation
			AggregationResults<Map> output = mongotemplate.aggregate(aggregation, "daily_time_spent_collection",
					Map.class);
			if (output.getMappedResults().size() > 0)
				for (Map<String, Object> temp : output.getMappedResults()) {
					ret.put(temp.get("day").toString(), temp.get("duration"));
				}
			else
				throw new Exception("No Data Found");
		} catch (Exception e) {
			ProjectLogger.log("Error in time spent repository: ", e, LoggerEnum.ERROR.name());
		}
		return ret;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Map<String, Object> getAvgDurationStats(Date startDate, Date endDate) {
		Map<String, Object> ret = new HashMap<>();
		try {
			MatchOperation matchStage = Aggregation.match(Criteria.where("date")
					.gte(LexJsonKey.formatter.format(startDate)).lte(LexJsonKey.formatter.format(endDate)));
			GroupOperation groupStage = Aggregation.group("date").sum("timespent").as("dur_date").addToSet("empMailId")
					.as("email_set");
			ProjectionOperation projectStage = Aggregation.project().andExpression("_id").as("day").and("dur_date")
					.divide(ArrayOperators.Size.lengthOfArray("email_set")).as("duration").andExclude("_id");

			Aggregation aggregation = Aggregation.newAggregation(matchStage, groupStage, projectStage);

			// aggregation
			AggregationResults<Map> output = mongotemplate.aggregate(aggregation, "daily_time_spent_collection",
					Map.class);
			if (output.getMappedResults().size() > 0)
				for (Map<String, Object> temp : output.getMappedResults()) {
					ret.put(temp.get("day").toString(), temp.get("duration"));
				}
			else
				throw new Exception("No Data Found");
		} catch (Exception e) {
			ProjectLogger.log("Error in time spent repository: ", e, LoggerEnum.ERROR.name());
		}
		return ret;
	}
}
