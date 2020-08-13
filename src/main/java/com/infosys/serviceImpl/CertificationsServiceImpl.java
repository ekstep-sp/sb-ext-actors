/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.http.client.ClientProtocolException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.sunbird.common.models.util.ProjectLogger;

import com.infosys.elastic.helper.ConnectionManager;
import com.infosys.exception.ApplicationLogicError;
import com.infosys.exception.BadRequestException;
import com.infosys.model.CerticationResponseBody;
import com.infosys.model.CertificationResponse;
import com.infosys.model.ContentMeta;
import com.infosys.service.CertificationsService;
import com.infosys.util.LexConstants;
import com.infosys.util.LexProjectUtil;
import com.infosys.util.Util;

@Service
public class CertificationsServiceImpl implements CertificationsService {

	@Autowired
	RestTemplate restTemplate;

	public static SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");

	public LinkedHashMap<String, ContentMeta> getCertifications(List<String> tracks, String sortOrder)
			throws IOException {

		BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();

		queryBuilder.must(QueryBuilders.termQuery(LexConstants.RESOURCE_TYPE, LexConstants.CERTIFICATION));
		queryBuilder.must(QueryBuilders.termQuery("status", "Live"));

		if (tracks != null && !tracks.isEmpty()) {
			queryBuilder.must(QueryBuilders.termsQuery(LexConstants.TRACK_NAME, tracks));
		}

		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.query(queryBuilder);
		if (sortOrder != null) {
			if (sortOrder.toLowerCase().equals("asc")) {
				sourceBuilder.sort(LexConstants.VERSION_DATE, SortOrder.ASC);
			} else {
				sourceBuilder.sort(LexConstants.VERSION_DATE, SortOrder.DESC);
			}
		} else {
			sourceBuilder.sort(LexConstants.VERSION_DATE, SortOrder.DESC);
		}

		sourceBuilder.size(100);
		SearchResponse response = ConnectionManager.getClient()
				.search(new SearchRequest().indices(LexProjectUtil.EsIndex.bodhi.getIndexName())
						.types(LexProjectUtil.EsType.resource.getTypeName()).searchType(SearchType.QUERY_THEN_FETCH)
						.source(sourceBuilder), RequestOptions.DEFAULT);

		LinkedHashMap<String, ContentMeta> responseMap = new LinkedHashMap<String, ContentMeta>();

		List<ContentMeta> checkList = new ArrayList<>();

		for (SearchHit hit : response.getHits()) {

			Map<String, Object> map = hit.getSourceAsMap();

			try {
				ContentMeta content = ContentMeta.fromMap(map);
				checkList.add(content);
				String contentIdAtSource = content.getContentIdAtSource();
				if (contentIdAtSource == null || contentIdAtSource.isEmpty()) {
					throw new ApplicationLogicError("Internal Server Error, Corrupt meta" + content.getIdentifier());
				}
				responseMap.put(contentIdAtSource, content);
			} catch (IllegalArgumentException ie) {
				ie.printStackTrace();
				throw new ApplicationLogicError(
						"Internal Server Error, Corrupt meta" + map.get("identifier").toString());
			}
		}
		return responseMap;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, List<ContentMeta>> getUserCertifications(Map<String, Object> reqMap)
			throws ParseException, ClientProtocolException, IOException {
		Map<String, List<ContentMeta>> responseMap = new HashMap<String, List<ContentMeta>>();
		String userEmail = null;
		List<String> tracks = (List<String>) reqMap.get(LexConstants.TRACKS);

		try {
			userEmail = reqMap.get(LexConstants.USER_EMAIL).toString();
		} catch (NullPointerException npe) {
			throw new BadRequestException("emailId cannot be null");
		}

		if (userEmail.isEmpty()) {
			throw new BadRequestException("userEmail is  empty");
		}

		Map<String, Object> request = new HashMap<String, Object>();
		request.put(LexConstants.USER_ID, userEmail);
		request.put(LexConstants.CLIENT_ID, LexConstants.REQUEST_CLIENT_ID);
		request.put(LexConstants.CLIENT_SECRET, LexConstants.REQUEST_CLIENT_SECRET);
		String iapUrl = this.getContentConfigarations();
		ResponseEntity<CerticationResponseBody> responseEntity = null;
		try {
			responseEntity = restTemplate.postForEntity(iapUrl, request, CerticationResponseBody.class);
		} catch (HttpServerErrorException httpServerErrorException) {
			httpServerErrorException.printStackTrace();
			throw new ApplicationLogicError("IAP Rest Call Exception");
		}
		CerticationResponseBody certificationResponseBody = responseEntity.getBody();
		HttpStatus statusCode = responseEntity.getStatusCode();

		if (statusCode.equals(HttpStatus.BAD_REQUEST)) {
			throw new BadRequestException("emailId sent does not exist in IAP");
		}

		if (statusCode.equals(HttpStatus.INTERNAL_SERVER_ERROR)) {
			throw new ApplicationLogicError("Error processing info from IAP");
		}

		Map<String, CertificationResponse> certificationResponseMap = new HashMap<String, CertificationResponse>();

		List<CertificationResponse> certificationResponses = certificationResponseBody.getResultList();

		for (CertificationResponse certificationResponse : certificationResponses) {
			if (certificationResponse.getCertificationCode() == null
					|| certificationResponse.getCertificationCode().isEmpty()) {
				throw new ApplicationLogicError("Corrupt IAP response");
			}
			certificationResponseMap.put(certificationResponse.getCertificationCode(), certificationResponse);
		}
		String sortOrder = (String) reqMap.get("sortOrder");
		LinkedHashMap<String, ContentMeta> certifications = getCertifications(tracks, sortOrder);

		List<ContentMeta> passedList = new ArrayList<ContentMeta>();
		List<ContentMeta> ongoingList = new ArrayList<ContentMeta>();
		List<ContentMeta> cannotAttemptList = new ArrayList<ContentMeta>();
		List<ContentMeta> canAttemptList = new ArrayList<ContentMeta>();
		List<ContentMeta> sortedList = new ArrayList<>();
		for (String contentId : certifications.keySet()) {
			ContentMeta content = certifications.get(contentId);
			CertificationResponse certificationResponse = certificationResponseMap.get(contentId);
			Double passPercentage = content.getPassPercentage();

			if (certificationResponse != null) {
				String status = certificationResponse.getStatus();

				if (status == null) {
					throw new ApplicationLogicError("IAP contract not adhered, no status field returned");
				}
				if (status.toLowerCase().equals(LexConstants.STATUS_ONGOING)) {
					if (sortOrder != null) {
						content.setCertificationStatus(LexConstants.STATUS_ONGOING);
						sortedList.add(content);
					}
					ongoingList.add(content);
				} else if (status.toLowerCase().equals(LexConstants.STATUS_SUBMITTED)) {
					Double score = certificationResponse.getScore();
					String submissionTime = certificationResponse.getEndTime();
					if (submissionTime == null) {
						throw new ApplicationLogicError("IAP Contract not adhered, No submission time");
					}
					Calendar subTime = Calendar.getInstance();
					subTime.setTime(formatter.parse(submissionTime));
					content.setRecentCerticationAttemptScore(score);
					content.setCertificationSubmissionDate(formatter.format(subTime.getTime()));
					if (score > passPercentage) {

						if (sortOrder != null) {
							content.setCertificationStatus(LexConstants.PASSED);
							sortedList.add(content);
						}
						passedList.add(content);
					} else {

						String currentTime = certificationResponseBody.getCurrentTime();
						if (currentTime == null) {
							throw new ApplicationLogicError("IAP Contract not adhered, No current time");
						}
						Calendar currTime = Calendar.getInstance();

						currTime.setTime(formatter.parse(currentTime));
						Date endDate = currTime.getTime();
						Date startDate = subTime.getTime();
						if ((endDate.getTime() - startDate.getTime()) / (1000f * 60 * 60 * 24) > 15) {
							if (sortOrder != null) {
								content.setCertificationStatus(LexConstants.CAN_ATTEMPT);
								sortedList.add(content);
							}
							canAttemptList.add(content);
						} else {
							if (sortOrder != null) {
								content.setCertificationStatus(LexConstants.CANNOT_ATTEMPT);
								subTime.add(Calendar.DATE, LexConstants.MIN_DAYS_BEFORE_NEXT_ATTEMPT);
								content.setNextCertificationAttemptDate(formatter.format(subTime.getTime()));
								sortedList.add(content);
							}
							cannotAttemptList.add(content);
						}
					}
				}
			} else {
				if (sortOrder != null) {
					content.setCertificationStatus(LexConstants.CAN_ATTEMPT);
					sortedList.add(content);
				}
				canAttemptList.add(content);
			}

		}

		responseMap.put(LexConstants.PASSED_LIST, passedList);
		responseMap.put(LexConstants.ONGOING_LIST, ongoingList);
		responseMap.put(LexConstants.CANNOT_ATTEMPT_LIST, cannotAttemptList);
		responseMap.put(LexConstants.CAN_ATTEMPT_LIST, canAttemptList);
		responseMap.put(LexConstants.SORTED_LIST, sortedList);

		return responseMap;

	}

	public String getContentConfigarations() {

		String ip = System.getenv(LexConstants.IAP_CERTIFICATION_URL);
		// Checking For ip and port in prop and system files
		if (ip == null) {
			ProjectLogger.log("from properties file");
			Properties prop = new Properties();
			InputStream is = Util.class.getResourceAsStream("/config.properties");
			try {
				prop.load(is);
			} catch (IOException e) {
				throw new ApplicationLogicError("properties file not found");
			}
			ip = prop.getProperty(LexConstants.IAP_CERTIFICATION_URL).trim();

		}
		return ip;
	}

}
