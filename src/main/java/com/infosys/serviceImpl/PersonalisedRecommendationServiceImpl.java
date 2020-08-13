/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infosys.service.PersonalisedRecommendationService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
public class PersonalisedRecommendationServiceImpl implements PersonalisedRecommendationService {

	public Map<String, Object> getPersonalisedCourses(String userId)
			throws JsonParseException, JsonMappingException, IOException {
		String json = "{\"count\":1,\"courses\": [{\"dateTime\": \"2017-11-28 06:16:44:444+0000\",\"identifier\": \"bb1ae73bd839d597cd55e76899f0968aae9d672defd8ce96766ca5d85e4cd3ac\",\"enrolledDate\": \"2017-11-28 06:16:44:445+0000\",\"contentId\": \"do_2123610248886435841486\",\"active\": true,\"description\": \"Oct 25\",\"courseLogoUrl\": \"https://ekstep-public-qa.s3-ap-south-1.amazonaws.com/content/do_2123610248886435841486/artifact/b5c2ff92ab5512754a24b7ed0a09e97f_1478082514640.thumb.jpeg\",\"batchId\": \"0123850742088663040\",\"userId\": \"eb35ce9e-3407-468f-81fa-3f99e9a2f533\",\"courseName\": \"1Course\",\"leafNodesCount\": 1, \"progress\": 0,\"id\": \"bb1ae73bd839d597cd55e76899f0968aae9d672defd8ce96766ca5d85e4cd3ac\",\"tocUrl\": \"https://ekstep-public-qa.s3-ap-south-1.amazonaws.com/content/do_2123610248886435841486/artifact/do_2123610248886435841486toc.json\",\"courseId\": \"do_2123610248886435841486\",\"status\": 0}]}";

		// have to implement these services to get personalised content n courses

		getPersonalisedCoursesFromImplicit();
		getPersonalisedCoursesFromARL();
		getPersonalisedCoursesFromInterestsInCompass();
		getPersonalisedCoursesFromClustering();

		ObjectMapper map = new ObjectMapper();

		Map<String, Object> newCourses = map.readValue(json, Map.class);

		return newCourses;
	}

	public void getPersonalisedCoursesFromImplicit() {
	};

	public void getPersonalisedCoursesFromARL() {
	};

	public void getPersonalisedCoursesFromInterestsInCompass() {
	};

	public void getPersonalisedCoursesFromClustering() {
	};

	
}
