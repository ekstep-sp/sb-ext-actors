/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infosys.service.LastViewedService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
public class LastViewedServiceImpl implements LastViewedService {

	public Map<String, Object> getLastViewedResourceDetailsOfUser(String userId)
			throws JsonParseException, JsonMappingException, IOException {

		String json = "{\"count\":1,\"content\":[{\"code\":\"org.sunbird.x9xnwY\",\"subject\":\"English\",\"channel\":\"505c7c48ac6dc1edc9b08f21db5a571d\",\"language\":[\"English\"],\"mimeType\":\"application/vnd.ekstep.ecml-archive\",\"idealScreenSize\":\"normal\",\"createdOn\":\"2017-11-27T04:45:11.336+0000\",\"objectType\":\"Content\",\"gradeLevel\":[\"Grade 4\",\"Grade 5\"],\"contentDisposition\":\"inline\",\"lastUpdatedOn\":\"2017-11-27T04:49:28.743+0000\",\"contentEncoding\":\"gzip\",\"contentType\":\"Resource\",\"identifier\":\"do_2123758658731868161528\",\"creator\":\"Anupama.\",\"createdFor\":[\"ORG_001\"],\"audience\":[\"Learner\"],\"IL_SYS_NODE_TYPE\":\"DATA_NODE\",\"visibility\":\"Default\",\"os\":[\"All\"],\"consumerId\":\"fa271a76-c15a-4aa1-adff-31dd04682a1f\",\"mediaType\":\"content\",\"osId\":\"org.ekstep.quiz.app\",\"graph_id\":\"domain\",\"nodeType\":\"DATA_NODE\",\"versionKey\":\"1511758174192\",\"idealScreenDensity\":\"hdpi\",\"createdBy\":\"3be2b92a-2d94-45c1-86d0-a66dc34e7d7e\",\"compatibilityLevel\":2,\"IL_FUNC_OBJECT_TYPE\":\"Content\",\"name\":\"Solar System\",\"IL_UNIQUE_ID\":\"do_2123758658731868161528\",\"board\":\"CBSE\",\"resourceType\":\"Learning / Study material\",\"status\":\"Live\",\"node_id\":68456,\"lastUpdatedBy\":\"e6f0ee9e-6ca2-4fc1-9843-432f4dc03668\",\"appIcon\":\"https://ekstep-public-qa.s3-ap-south-1.amazonaws.com/content/do_2123758658731868161528/artifact/solarsystem-2_1510726418192.thumb.jpg\",\"description\":\"This course gives an overview of our Solar System.\",\"concepts\":[\"SC6\"],\"lastSubmittedOn\":\"2017-11-27T04:45:50.917+0000\",\"lastPublishedBy\":\"e6f0ee9e-6ca2-4fc1-9843-432f4dc03668\",\"downloadUrl\":\"https://ekstep-public-qa.s3-ap-south-1.amazonaws.com/ecar_files/do_2123758658731868161528/solar-system_1511758173588_do_2123758658731868161528_2.0.ecar\",\"variants\":{\"spine\":{\"ecarUrl\":\"https://ekstep-public-qa.s3-ap-south-1.amazonaws.com/ecar_files/do_2123758658731868161528/solar-system_1511758173895_do_2123758658731868161528_2.0_spine.ecar\",\"size\":153934}},\"posterImage\":\"https://ekstep-public-qa.s3-ap-south-1.amazonaws.com/content/do_2123758708134543361529/artifact/solarsystem-2_1510726418192.jpg\",\"pkgVersion\":2,\"s3Key\":\"ecar_files/do_2123758658731868161528/solar-system_1511758173588_do_2123758658731868161528_2.0.ecar\",\"size\":2811799,\"lastPublishedOn\":\"2017-11-27T04:49:33.588+0000\",\"artifactUrl\":\"https://ekstep-public-qa.s3-ap-south-1.amazonaws.com/content/do_2123758658731868161528/artifact/1511758173095_do_2123758658731868161528.zip\",\"SYS_INTERNAL_LAST_UPDATED_ON\":\"2017-11-27T04:49:34.192+0000\",\"appId\":\"sunbird_portal\",\"collections\":[],\"es_metadata_id\":\"do_2123758658731868161528\"}]},";

		ObjectMapper map = new ObjectMapper();

		Map<String, Object> lastViewedResources = map.readValue(json, Map.class);

		return lastViewedResources;

	}

	@Override
	public Map<String, Object> getLastViewedCourseDetailsOfUser(String userId)
			throws JsonParseException, JsonMappingException, IOException {

		String json = "{ \"count\" : 1,   \"courses\": [{\"dateTime\": \"2017-11-28 06:16:44:444+0000\",\"identifier\": \"bb1ae73bd839d597cd55e76899f0968aae9d672defd8ce96766ca5d85e4cd3ac\",\"enrolledDate\": \"2017-11-28 06:16:44:445+0000\",\"contentId\": \"do_2123610248886435841486\",\"active\": true,\"description\": \"Oct 25\",\"courseLogoUrl\": \"https://ekstep-public-qa.s3-ap-south-1.amazonaws.com/content/do_2123610248886435841486/artifact/b5c2ff92ab5512754a24b7ed0a09e97f_1478082514640.thumb.jpeg\",\"batchId\": \"0123850742088663040\",\"userId\": \"eb35ce9e-3407-468f-81fa-3f99e9a2f533\",\"courseName\": \"1Course\",\"leafNodesCount\": 1, \"progress\": 0,\"id\": \"bb1ae73bd839d597cd55e76899f0968aae9d672defd8ce96766ca5d85e4cd3ac\",\"tocUrl\": \"https://ekstep-public-qa.s3-ap-south-1.amazonaws.com/content/do_2123610248886435841486/artifact/do_2123610248886435841486toc.json\",\"courseId\": \"do_2123610248886435841486\",\"status\": 0}]}";

		ObjectMapper map = new ObjectMapper();

		Map<String, Object> lastViewedCourses = map.readValue(json, Map.class);

		return lastViewedCourses;
	}
}
