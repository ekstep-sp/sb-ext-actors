/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;

import com.infosys.elastic.common.ElasticSearchUtil;
import com.infosys.exception.ApplicationLogicError;
import com.infosys.exception.BadRequestException;
import com.infosys.exception.ElasticSearchException;
import com.infosys.exception.ResourceNotFoundException;
import com.infosys.model.ContentMeta;
import com.infosys.model.ObjectMeta;
import com.infosys.service.ViewCountService;
import com.infosys.util.ElasticUtil;
import com.infosys.util.LexJsonKey;
import com.infosys.util.LexProjectUtil;

@Service
public class ViewCountServiceImpl implements ViewCountService {

	@Autowired
	RestTemplate restTemplate;
	
	public void getAncestors(String resourceId, Set<String> identifiers, boolean flag) throws IOException {
		if (resourceId == null || resourceId.isEmpty()) {
			throw new BadRequestException("resourceId cannot be null or empty");
		}

		Map<String, Object> resourceMap = ElasticSearchUtil.getDataByIdentifier(
				LexProjectUtil.EsIndex.bodhi.getIndexName(), LexProjectUtil.EsType.resource.getTypeName(), resourceId);

		if (resourceMap == null || resourceMap.isEmpty()) {
			if (flag) {
				throw new ResourceNotFoundException(resourceId + " not found");
			} else {
				return;
			}
		}

		ContentMeta contentMeta = ContentMeta.fromMap(resourceMap);

		if (contentMeta == null) {
			if (flag) {
				throw new ResourceNotFoundException(resourceId + " not found");
			} else {
				return;
			}
		}

		ObjectMeta[] parents = contentMeta.getCollections();

		identifiers.add(resourceId);

		for (ObjectMeta parent : parents) {
			getAncestors(parent.getIdentifier(), identifiers, flag);
		}
	}

	public void updateViewCount(String resourceId) throws IOException {

		// System.out.println("Start Time: " + System.currentTimeMillis());

		Set<String> ancestors = new HashSet<String>();

		ancestors.add(resourceId);

		getAncestors(resourceId, ancestors, false);

		String ancestorsArray[] = ancestors.toArray(new String[ancestors.size()]);

		Map<String, Object> identifiersMap = new HashMap<String, Object>();
		Map<String, Object> termsMap = new HashMap<String, Object>();
		Map<String, Object> inlineMap = new HashMap<String, Object>();
		Map<String, Object> reqMap = new HashMap<String, Object>();

		identifiersMap.put(LexJsonKey.IDENTIFIER_KEYWORD, ancestorsArray);
		termsMap.put(LexJsonKey.TERMS, identifiersMap);

		inlineMap.put(LexJsonKey.INLINE, LexJsonKey.CONTEXT_SRC + "['" + LexJsonKey.SESSIONS_COUNT + "']="
				+ LexJsonKey.CONTEXT_SRC + "['" + LexJsonKey.SESSIONS_COUNT + "']+" + LexJsonKey.VIEW_COUNT_NUMBER);

		reqMap.put(JsonKey.QUERY, termsMap);
		reqMap.put(LexJsonKey.SCRIPT, inlineMap);

		try {
			String url = ElasticUtil.getEndPointForES(LexProjectUtil.EsIndex.bodhi.getIndexName(),
					LexProjectUtil.EsType.resource.getTypeName(), LexJsonKey.IP_FILE);

			url += LexJsonKey.UPDATE_BY_QUERY + "?conflicts=proceed";

			JSONObject jsonObject = new JSONObject(reqMap);

			// System.out.println("url is " + url);
			// System.out.println(jsonObject.toString());

			ResponseEntity<String> response = restTemplate.postForEntity(url, jsonObject.toString(), String.class);

			if (response.getStatusCodeValue() != 200) {
				throw new ApplicationLogicError("Error processing request");
			}
		} catch (ElasticSearchException ese) {
			throw new ApplicationLogicError(ese.getMessage());
		}
		catch(HttpStatusCodeException httpe)
		{
			ProjectLogger.log("HTTP-Status Exception encountered for resource-id " + resourceId + "/n" +
					          "End point : /v1/addView/"+resourceId  
					);
			throw httpe;
		}
		
		// System.out.println("Stop Time: " + System.currentTimeMillis());
	}

	/*
	 * @Override public boolean updateViewCount(String resourceId) { try {
	 * 
	 * Map<String, Object> result =
	 * ElasticSearchUtil.getDataByIdentifier(LexProjectUtil.EsIndex.bodhi.
	 * getIndexName( ), LexProjectUtil.EsType.resource.getTypeName(), resourceId);
	 * 
	 * if (result == null || result.isEmpty()) throw new
	 * ResourceNotFoundException("Resource with specified identifier not found");
	 * 
	 * Integer viewCount = (Integer) result.get("me_totalSessionsCount");
	 * 
	 * List<Map<String, Object>> parentList = (List<Map<String, Object>>)
	 * result.get("collections"); Deque<Map<String, Object>> parents = new
	 * ArrayDeque<Map<String, Object>>(); parents.addAll(parentList);
	 * 
	 * while (!parents.isEmpty()) { Map<String, Object> parent = parents.poll();
	 * String parentIdentifier = (String) parent.get("identifier");
	 * 
	 * 
	 * Map<String, Object> parentResult =
	 * ElasticSearchUtil.getDataByIdentifier(LexProjectUtil.EsIndex.bodhi.
	 * getIndexName( ), LexProjectUtil.EsType.resource.getTypeName(),
	 * parentIdentifier);
	 * 
	 * if (parentResult == null || parentResult.isEmpty()) throw new
	 * ResourceNotFoundException("Resource with specified identifier not found");
	 * 
	 * Integer parentViewCount = (Integer)
	 * parentResult.get("me_totalSessionsCount");
	 * 
	 * parentViewCount += 1;
	 * 
	 * parentResult.put("me_totalSessionsCount", parentViewCount); List<Map<String,
	 * Object>> ancestors = (List<Map<String, Object>>)
	 * parentResult.get("collections");
	 * 
	 * if (ancestors != null && !ancestors.isEmpty() && ancestors.size() > 0)
	 * parents.addAll(ancestors);
	 * 
	 * ElasticSearchUtil.upsertData(LexProjectUtil.EsIndex.bodhi.getIndexName(),
	 * LexProjectUtil.EsType.resource.getTypeName(), parentIdentifier,
	 * parentResult); }
	 * 
	 * viewCount += 1; result.put("me_totalSessionsCount", viewCount);
	 * 
	 * ElasticSearchUtil.upsertData(LexProjectUtil.EsIndex.bodhi.getIndexName(),
	 * LexProjectUtil.EsType.resource.getTypeName(), resourceId, result);
	 * 
	 * return true; } catch (Exception e) { return false; } }
	 */

	@Override
	public Integer getViewCount(String resourceId) throws IOException {

		Map<String, Object> resourceMeta = ElasticSearchUtil.getDataByIdentifier(
				LexProjectUtil.EsIndex.bodhi.getIndexName(), LexProjectUtil.EsType.resource.getTypeName(), resourceId);

		if (resourceMeta == null || resourceMeta.isEmpty())
			throw new ResourceNotFoundException("No such Identifier");

		Integer viewCount = (Integer) resourceMeta.get("me_totalSessionsCount");

		if (viewCount == null)
			throw new ApplicationLogicError("Database population error");

		return viewCount;
	}
	
	
	
}
