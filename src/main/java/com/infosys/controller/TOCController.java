/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import com.infosys.model.ContentMeta;
import com.infosys.model.ObjectMeta;
import com.infosys.util.LexProjectUtil;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.request.Request;
import com.infosys.elastic.common.ElasticSearchUtil;

import javax.net.ssl.*;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.*;

//import org.json.simple.JSONObject;


@RestController
public class TOCController {

    @Autowired
    RestTemplate restTemplate;

    private static void disableSslVerification() {
        try {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }};

            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
    }


	@GetMapping("/v1/nested/course/hierarchy/{courseId}")
	public List<Map<String, ContentMeta>> nestedHierarchy(@PathVariable("courseId") String courseId) throws IOException {

	/*@GetMapping("/v1/nested/course/hierarchy/{courseId}")
    public List<Map<String,ArrayList>> nestedHierarchy(@PathVariable("courseId") String courseId) {*/

		disableSslVerification();
		Queue<String> queue = new ArrayDeque<String>();
		queue.add(courseId);
		
		List<Map<String, ContentMeta>> courseChildMappingList = new ArrayList<Map<String, ContentMeta>>();

		List<String> uniqueIdentifiers = new ArrayList<String>();

		while (!queue.isEmpty()) {
			
			System.out.println(queue.size());

			String courseIdentifier = queue.poll();

			Map<String, ContentMeta> courseChildMapping = new HashMap<String, ContentMeta>();

			//courseChildMapping.put("resourceIdentifier", courseIdentifier);

			System.out.println(courseIdentifier);

			Map<String,Object> courseDetailsResponseForPage = getCourseMeta(courseIdentifier);

			ContentMeta contentMeta = ContentMeta.fromMap(courseDetailsResponseForPage);

			ObjectMeta[] children = contentMeta.getChildren();

			//ArrayList<LinkedHashMap> children = (ArrayList<LinkedHashMap>) content.get("children");
             if(children.length > 0)
             {
				for (ObjectMeta child : children) {
					String identifier = child.getIdentifier();
					if (!uniqueIdentifiers.contains(identifier))
						queue.add(identifier);
				}
             }
			
			System.out.println(contentMeta.toString());	
				
			courseChildMapping.put("bodhiContent", contentMeta);

			courseChildMappingList.add(courseChildMapping);
		}
		return courseChildMappingList;
	}

	/*public ResponseEntity<JSONObject> getCourseDetails(String courseId) {

		disableSslVerification();

		HttpHeaders headers = new HttpHeaders();

		PropertiesCache properties = PropertiesCache.getInstance();

		String bearer = properties.getProperty("bearer");

		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		headers.add("Authorization", bearer);

		HttpEntity<String> entity = new HttpEntity<String>("params", headers);

		String course_details_url = properties.getProperty("course_details_url");

		course_details_url += "/" + courseId;

		ResponseEntity<JSONObject> contentCreationResponse = restTemplate.exchange(course_details_url, HttpMethod.GET,
				entity, JSONObject.class);

		return contentCreationResponse;
	}*/
	
	public Map<String,Object> getCourseMeta(String courseId) throws IOException
	{

        Map<String, Object> mappedObject = ElasticSearchUtil.getDataByIdentifier(LexProjectUtil.EsIndex.bodhi.getIndexName(), LexProjectUtil.EsType.resource.getTypeName(), courseId);
        return mappedObject;

    }

    public ResponseEntity<JSONObject> getCourseDetails(String courseId) {

        disableSslVerification();

        HttpHeaders headers = new HttpHeaders();

        PropertiesCache properties = PropertiesCache.getInstance();

        String bearer = properties.getProperty("bearer");

        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", bearer);

        HttpEntity<String> entity = new HttpEntity<String>("params", headers);

        String course_details_url = properties.getProperty("course_details_url");

        course_details_url += "/" + courseId;

        ResponseEntity<JSONObject> contentCreationResponse = restTemplate.exchange(course_details_url, HttpMethod.GET, entity, JSONObject.class);

        return contentCreationResponse;
    }

    /* A Controller To get Title Name of Resouces of Givel LexIds 
     * */
    @PostMapping("/v1/getContentTitle")
    @SuppressWarnings("unchecked")
    Response  getCouseDetails(@RequestBody Request requestBody) throws IOException
    {
    	Response response = new Response();
    	Map<String, Object> request = requestBody.getRequest(); 
    	List<String> lexIds=(List<String>)request.get("Ids");
		response.setVer("v1"); 
		response.setId("api.toc.contentTitles");
		response.setTs(ProjectUtil.getFormattedDate());
		List<String> invalidIds = new ArrayList<>();
		List<Map<String,Object>> validIds = new ArrayList<>();
		for(String id : lexIds) {
			Map<String,Object> contentMeta = getCourseMeta(id);
			if(contentMeta.isEmpty()){
				invalidIds.add(id);
			}
			else {
				Map<String,Object> content = new HashMap<String, Object>();
				content.put("lex_id",id);
				content.put("title",contentMeta.get("name"));
				validIds.add(content);
			}
		}
		response.put("validIds",validIds);
		response.put("invalidIds",invalidIds);
		return response;
    }
}

