/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infosys.service.EnrollmentService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
public class EnrolmentServiceImpl implements EnrollmentService{

	public Map<String,Object> getUserEnrolledCourses(String userId) {
			
	//String json = \"{\\"courses\\": [{\\"dateTime\\": \\"2017-11-28 06:16:44:444+0000\\",  \\"identifier\\": \\"bb1ae73bd839d597cd55e76899f0968aae9d672defd8ce96766ca5d85e4cd3ac\\", \\"enrolledDate\\": \\"2017-11-28 06:16:44:445+0000\\",  \\"contentId\\": \\"do_2123610248886435841486\\", \\"active\\": true,\\"description\\": \\"Oct 25\\",\\"courseLogoUrl\\": \\"https://ekstep-public-qa.s3-ap-south-1.amazonaws.com/content/do_2123610248886435841486/artifact/b5c2ff92ab5512754a24b7ed0a09e97f_1478082514640.thumb.jpeg\\",\\"batchId\\": \\"0123850742088663040\\", \\"userId\\": \\"eb35ce9e-3407-468f-81fa-3f99e9a2f533\\",\\"courseName\\": \\"1Course\\",\\"leafNodesCount\\": 1,\\"progress\\": 0,\\"id\\": \\"bb1ae73bd839d597cd55e76899f0968aae9d672defd8ce96766ca5d85e4cd3ac\\",\\"tocUrl\\": \\"https://ekstep-public-qa.s3-ap-south-1.amazonaws.com/content/do_2123610248886435841486/artifact/do_2123610248886435841486toc.json\\",\\"courseId\\": \\"do_2123610248886435841486\\",\\"status\\": 0},{\\"dateTime\\": \\"2017-11-28 05:27:37:491+0000\\",\\"identifier\\": \\"9cef922a92f577231d32d370f3f066406d1910a8ad2afd17e2e5f5013ecde702\\",\\"lastReadContentStatus\\": 2,\\"enrolledDate\\": \\"2017-11-17 06:47:16:323+0000\\",\\"contentId\\": \\"do_2123772810629365761693\\",\\"active\\": true,\\"description\\": \\"Course for the Test1 Assessment\\",\\"courseLogoUrl\\": \\"https://ekstep-public-qa.s3-ap-south-1.amazonaws.com/content/do_2123772810629365761693/artifact/1-1_1485252935201.thumb.png\\",\\"batchId\\": \\"01237730882962227248\\", \\"userId\\": \\"eb35ce9e-3407-468f-81fa-3f99e9a2f533\\",\\"courseName\\": \\"Test1 Course\\",\\"leafNodesCount\\": 2,\\"progress\\": 1,\\"id\\": \\"9cef922a92f577231d32d370f3f066406d1910a8ad2afd17e2e5f5013ecde702\\",\\"tocUrl\\": \\"https://ekstep-public-qa.s3-ap-south-1.amazonaws.com/content/do_2123772810629365761693/artifact/do_2123772810629365761693toc.json\\",\\"lastReadContentId\\": \\"do_2123772594434457601688\\",\\"courseId\\": \\"do_2123772810629365761693\\",\\"status\\": 1}, {\\"dateTime\\": \\"2017-11-29 05:33:42:856+0000\\",\\"identifier\\": \\"a9bbbee0b9a29c3b75e872408f55ac7e9be328415faf3b165fcb4e3aff57e804\\",\\"enrolledDate\\": \\"2017-11-29 05:33:42:856+0000\\",\\"contentId\\": \\"do_2123610248886435841486\\",\\"active\\": true,\\"description\\": \\"Oct 25\\",\\"courseLogoUrl\\": \\"https://ekstep-public-qa.s3-ap-south-1.amazonaws.com/content/do_2123610248886435841486/artifact/b5c2ff92ab5512754a24b7ed0a09e97f_1478082514640.thumb.jpeg\\",\\"batchId\\": \\"01238576609880473621\\",\\"userId\\": \\"eb35ce9e-3407-468f-81fa-3f99e9a2f533\\",\\"courseName\\": \\"1Course\\",\\"leafNodesCount\\": 1,\\"progress\\": 0,\\"id\\": \\"a9bbbee0b9a29c3b75e872408f55ac7e9be328415faf3b165fcb4e3aff57e804\\",\\"tocUrl\\": \\"https://ekstep-public-qa.s3-ap-south-1.amazonaws.com/content/do_2123610248886435841486/artifact/do_2123610248886435841486toc.json\\",\\"courseId\\": \\"do_2123610248886435841486\\",\\"status\\": 0},{\\"dateTime\\": \\"2017-11-29 11:15:49:081+0000\\",\\"identifier\\": \\"842c4fd1319dde1673ee02eca6a22a291a4ad031072906ccc456fc3cc02b6969\\",\\"lastReadContentStatus\\": 1,\\"enrolledDate\\": \\"2017-11-29 05:34:23:374+0000\\",\\"contentId\\": \\"do_2123845009112514561530\\",\\"active\\": true,\\"description\\": \\"Course on NoSQL\\",\\"courseLogoUrl\\": \\"https://ekstep-public-qa.s3-ap-south-1.amazonaws.com/content/do_2123845009112514561530/artifact/1-1_1485252935201.thumb.png\\",\\"batchId\\": \\"01238576828263628828\\",\\"userId\\": \\"eb35ce9e-3407-468f-81fa-3f99e9a2f533\\",\\"courseName\\": \\"Parimala_Course\\",\\"leafNodesCount\\": 2,\\"progress\\": 0,\\"id\\": \\"842c4fd1319dde1673ee02eca6a22a291a4ad031072906ccc456fc3cc02b6969\\",\\"tocUrl\\": \\"https://ekstep-public-qa.s3-ap-south-1.amazonaws.com/content/do_2123845009112514561530/artifact/do_2123845009112514561530toc.json\\",\\"lastReadContentId\\": \\"do_2123844941606338561521\\",\\"courseId\\": \\"do_2123845009112514561530\\"\\"status\\": 1 },{\\"dateTime\\": \\"2017-11-27 12:46:28:824+0000\\",\\"identifier\\": \\"7dd007207931a4235c3202f9e81360141a6cb20e07d33d1dd1beefb7f85572d5\\",\\"lastReadContentStatus\\": 1,\\"enrolledDate\\": \\"2017-11-20 13:38:19:109+0000\\",\\"contentId\\": \\"do_2123796061684039681776\\",\\"active\\": true,\\"description\\": \\"batch testing\\",\\"courseLogoUrl\\": \\"https://ekstep-public-qa.s3-ap-south-1.amazonaws.com/content/do_2123796061684039681776/artifact/beee6757847b84a1e41ce827ae02ccc7_1477485749628.thumb.jpeg\\",\\"batchId\\": \\"0123796363332157443\\",\\"userId\\": \\"eb35ce9e-3407-468f-81fa-3f99e9a2f533\\",\\"courseName\\": \\"batch testing course\\",\\"leafNodesCount\\": 3,\\"progress\\": 0,   \\"id\\": \\"7dd007207931a4235c3202f9e81360141a6cb20e07d33d1dd1beefb7f85572d5\\",\\"tocUrl\\": \\"https://ekstep-public-qa.s3-ap-south-1.amazonaws.com/content/do_2123796061684039681776/artifact/do_2123796061684039681776toc.json\\",\\"lastReadContentId\\": \\"do_212305818467631104131\\",\\"courseId\\": \\"do_2123796061684039681776\\",\\"status\\": 1 }, {\\"dateTime\\": \\"2017-11-29 06:46:15:107+0000\\",\\"identifier\\": \\"36c37117100e49a3632f18520bcd4492fbb5636ef423e7e0b2725ba59eb5d982\\",\\"lastReadContentStatus\\": 1,\\"enrolledDate\\": \\"2017-11-21 05:17:18:710+0000\\",\\"contentId\\": \\"do_2123774889185689601736\\",\\"active\\": true,\\"description\\": \\"Created to ace in web UI development\\",\\"courseLogoUrl\\": \\"https://ekstep-public-qa.s3-ap-south-1.amazonaws.com/content/do_2123774889185689601736/artifact/a2b1d5cf96ad28f15e79df61dbb21fdf_1478083821843.thumb.jpeg\\",\\"batchId\\": \\"01238009439243468811\\",\\"userId\\": \\"eb35ce9e-3407-468f-81fa-3f99e9a2f533\\",\\"courseName\\": \\"Angular Tutorials\\",\\"leafNodesCount\\": 3,\\"progress\\": 4\\"id\\": \\"36c37117100e49a3632f18520bcd4492fbb5636ef423e7e0b2725ba59eb5d982\\",\\"tocUrl\\": \\"https://ekstep-public-qa.s3-ap-south-1.amazonaws.com/content/do_2123774889185689601736/artifact/do_2123774889185689601736toc.json\\",\\"lastReadContentId\\": \\"do_2123844851695370241514\\",\\"courseId\\": \\"do_2123774889185689601736\\",\\"status\\": 2 }] }\" ;
	
    String json = "{\"courses\": [{\"dateTime\": \"2017-11-28 06:16:44:444+0000\",\"identifier\": \"bb1ae73bd839d597cd55e76899f0968aae9d672defd8ce96766ca5d85e4cd3ac\",\"enrolledDate\": \"2017-11-28 06:16:44:445+0000\",\"contentId\": \"do_2123610248886435841486\",\"active\": true,\"description\": \"Oct 25\",\"courseLogoUrl\": \"https://ekstep-public-qa.s3-ap-south-1.amazonaws.com/content/do_2123610248886435841486/artifact/b5c2ff92ab5512754a24b7ed0a09e97f_1478082514640.thumb.jpeg\",\"batchId\": \"0123850742088663040\",\"userId\": \"eb35ce9e-3407-468f-81fa-3f99e9a2f533\",\"courseName\": \"1Course\",\"leafNodesCount\": 1, \"progress\": 0,\"id\": \"bb1ae73bd839d597cd55e76899f0968aae9d672defd8ce96766ca5d85e4cd3ac\",\"tocUrl\": \"https://ekstep-public-qa.s3-ap-south-1.amazonaws.com/content/do_2123610248886435841486/artifact/do_2123610248886435841486toc.json\",\"courseId\": \"do_2123610248886435841486\",\"status\": 0}]}";
	
	ObjectMapper map = new ObjectMapper();
	
	try {
		return map.readValue(json,Map.class);
	} catch (JsonParseException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (JsonMappingException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
	return null;
}
	
}
