/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.sunbird.common.models.util.ProjectLogger;

import com.infosys.elastic.helper.ConnectionManager;
import com.infosys.exception.ApplicationLogicError;
import com.infosys.exception.InvalidDataInputException;
import com.infosys.model.ContentProgress;
import com.infosys.model.cassandra.ContentProgressModel;
import com.infosys.model.cassandra.ContentProgressPrimaryKeyModel;
import com.infosys.model.cassandra.UserHistoryModel;
import com.infosys.model.cassandra.UserHistoryPrimaryKeyModel;
import com.infosys.repository.ContentProgressRepository;
import com.infosys.repository.UserRepository;
import com.infosys.service.ContentProgressService;
import com.infosys.service.DiscretePointsService;
//import com.infosys.util.Constants;

@Service
public class ContentProgressServiceImpl implements ContentProgressService {

	@Autowired
	ContentProgressRepository contentProgressRepo;

	@Autowired
	UserRepository userRepo;

	@Autowired
	DiscretePointsService discretePointService;

	@Async("progressExecutor")
	@SuppressWarnings("unchecked")
	@Override
	public String updateProgress(String rootOrg,String userId, String contentId, Map<String, Object> data) throws Exception {
		try {
			Boolean updateOnlyHistory = false;
			Boolean completedAssessment = false;

			String userUUID = "";
			if (data.get("user_id_type").toString().toLowerCase().equals("email")) {
				Map<String, Object> temp = userRepo.getUUIDFromEmail(userId);
				userUUID = temp.get("id").toString();
			} else {
				userUUID = userId;
				if (userUUID == null || userUUID.isEmpty())
					throw new InvalidDataInputException("Empty User Id");
				Map<String, Object> temp = userRepo.getEmailFromUUID(userUUID);
				if (temp.isEmpty())
					throw new InvalidDataInputException("Invalid User Id");
			}

			Map<String, ContentProgressModel> meta = new HashMap<>();
			Map<String, ContentProgress> contentProgressMap = new HashMap<>();
			List<UserHistoryModel> history = new ArrayList<>();
			// resource block for meta and hierarchy(1 es call 1 cas call)
			{
				Map<String, Object> temp = this.getMetaForResource(rootOrg,contentId, userUUID);
				ContentProgressModel resourceProgress = contentProgressRepo.findById(
						new ContentProgressPrimaryKeyModel(rootOrg,userUUID, data.get("content_type").toString(), contentId))
						.orElse(null);
				meta.putAll((Map<String, ContentProgressModel>) temp.get("meta"));
				float oldProgress = 0f;
				String maxSize = data.get("max_size").toString();
				List<String> current = (List<String>) data.get("current");
				String mimeType = data.get("mime_type").toString();
				Set<String> progressPages = new HashSet<>();
				if (resourceProgress != null) {
					oldProgress = resourceProgress.getProgress();
					progressPages = resourceProgress.getVisitedSet() != null ? resourceProgress.getVisitedSet()
							: new HashSet<>();
				} else {
					meta.get(contentId).setFirstAccessedOn(new Date());
				}
				if (oldProgress != 1f) {
					float newProgress = oldProgress;
					// page based
					if (mimeType.toLowerCase().equals("application/web-module")
							|| mimeType.toLowerCase().equals("application/pdf")) {
						progressPages.addAll(current);
						newProgress = ((float) progressPages.size()) / Float.parseFloat(maxSize);
					}
					// time based
					else if (mimeType.toLowerCase().equals("video/interactive")
							|| mimeType.toLowerCase().equals("video/mp4")
							|| mimeType.toLowerCase().equals("video/x-youtube")
							|| mimeType.toLowerCase().equals("audio/mpeg")
							|| mimeType.toLowerCase().equals("application/html")) {
						float previousTime = !progressPages.isEmpty()
								? Float.parseFloat(progressPages.toArray(new String[1])[0])
								: 0;
						if (Float.parseFloat(current.get(0)) > previousTime) {
							progressPages = new HashSet<>();
							progressPages.addAll(current);
							newProgress = Float.parseFloat(current.get(0)) / Float.parseFloat(maxSize);
							if (newProgress > 0.95f) {
								newProgress = 1f;
							}
						}
					}
					// result based
					else if (mimeType.toLowerCase().equals("application/quiz")
							|| mimeType.toLowerCase().equals("application/iap-assessment")) {
						float previousResult = !progressPages.isEmpty()
								? Float.parseFloat(progressPages.toArray(new String[1])[0])
								: 0;
						if (Float.parseFloat(current.get(0)) > previousResult) {
							progressPages = new HashSet<>();
							progressPages.addAll(current);
							if (Float.parseFloat(current.get(0)) > 60) {
								newProgress = 1;
								if (temp.get("resource_type").toString().toLowerCase().equals("assessment"))
									completedAssessment = true;
							}
						}
					}

					if (resourceProgress != null && (newProgress <= oldProgress))
						updateOnlyHistory = true;
					if (newProgress >= 1f) {
						newProgress = 1;
						meta.get(contentId).setFirstCompletedOn(new Date());
					}
					meta.get(contentId).setProgress(newProgress);
					meta.get(contentId).setVisitedSet(progressPages);
					meta.get(contentId).setLastTS(new Date());
				} else {
					meta.get(contentId).setProgress(oldProgress);
					updateOnlyHistory = true;
				}
				contentProgressMap.put(contentId, new ContentProgress(temp.get("resource_type").toString(),
						meta.get(contentId).getProgress(), Long.parseLong(temp.get("duration").toString())));
				history.add(new UserHistoryModel(new UserHistoryPrimaryKeyModel(userUUID, contentId), new Date()));
			}
			// meta for the hierarchy(es calls=Level number-1 and 1 cas call)
			{
				Map<String, Object> temp = this.getMetaAndProgress(rootOrg,meta.get(contentId).getParentList(), userUUID,
						updateOnlyHistory, completedAssessment);
				meta.putAll((Map<String, ContentProgressModel>) temp.get("meta"));
				contentProgressMap.putAll((Map<String, ContentProgress>) temp.get("content_progress"));
				if (!updateOnlyHistory)
					discretePointService.PutPoints(userUUID, contentId,
							contentProgressMap.get(contentId).getResourceType(), temp.get("parent").toString());
			}

			List<String> updateProgressList = new ArrayList<>();
			updateProgressList.addAll(meta.get(contentId).getParentList());

			while (!updateProgressList.isEmpty()) {
				List<String> nextProgressList = new ArrayList<String>();
				for (String id : updateProgressList) {
					float oldProgress = 0f;
					if (meta.containsKey(id)) {
						if (contentProgressMap.get(id) != null) {
							if (contentProgressMap.get(id).getProgress() != -1)
								oldProgress = contentProgressMap.get(id).getProgress();
							else
								meta.get(id).setFirstAccessedOn(new Date());
							if (oldProgress != 1f) {
								float newProgress = oldProgress;
								if (completedAssessment)
									newProgress = 1;
								else {
									if (meta.get(id).getProgress() == null)
										newProgress = this.getProgress(meta.get(id).getChildrenList(),
												contentProgressMap, meta, 0);
									else
										newProgress = meta.get(id).getProgress();
								}
								meta.get(id).setProgress(newProgress);
								meta.get(id).setLastTS(new Date());
								if (newProgress == 1f)
									meta.get(id).setFirstCompletedOn(new Date());
							}
						}
						nextProgressList.addAll(meta.get(id).getParentList());
						history.add(new UserHistoryModel(new UserHistoryPrimaryKeyModel(userUUID, id), new Date()));
					} else
						throw new ApplicationLogicError("Created Wrong Hierarchy");
				}
				updateProgressList = nextProgressList;
			}

			contentProgressRepo.updateProgress(updateOnlyHistory ? new ArrayList<>() : meta.values(), history);
		} catch (InvalidDataInputException e) {
			throw e;
		} catch (ApplicationLogicError e) {
			throw e;
		} catch (Exception e) {
			throw new Exception(e.getMessage());
		}
		return "Success";
	}

	private Float getProgress(List<String> childrenList, Map<String, ContentProgress> contentProgressMap,
			Map<String, ContentProgressModel> contentMeta, int counter) throws Exception {
		float sum = 0;
		float duration = 0;
		Boolean hasAssessment = false;
		// recursion counter
		if (counter >= 10) {
			throw new Exception("Invalid Hirarchy");
		}
		for (String child : childrenList) {
			if (!contentProgressMap.get(child).getResourceType().toLowerCase().equals("assessment")) {
				if (contentMeta.get(child) != null) {
					if (contentMeta.get(child).getProgress() != null) {
						sum += contentMeta.get(child).getProgress() * contentProgressMap.get(child).getDuration();
						duration += contentProgressMap.get(child).getDuration();
					} else {
						float childProgress = this.getProgress(contentMeta.get(child).getChildrenList(),
								contentProgressMap, contentMeta, counter + 1);
						contentMeta.get(child).setProgress(childProgress);
						sum += childProgress * contentProgressMap.get(child).getDuration();
						duration += contentProgressMap.get(child).getDuration();
					}
				} else {
					sum += contentProgressMap.get(child).getProgress() * contentProgressMap.get(child).getDuration();
					duration += contentProgressMap.get(child).getDuration();
				}
			} else
				hasAssessment = true;
		}
		return hasAssessment ? (sum / duration) * 0.7f : (sum / duration);
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getMetaAndProgress(String rootOrg,List<String> contentParentList, String userUUID,
			Boolean updateOnlyHistory, Boolean completedAssessment) throws IOException {
		Map<String, Object> ret = new HashMap<>();
		// meta for the hierarchy
		Map<String, ContentProgressModel> meta = new HashMap<>();
		// progress, duration and resourceType for all the ids
		Map<String, ContentProgress> contentProgressMap = new HashMap<>();

		Map<String, Object> hierarchy = getHierarchyForResource(rootOrg,contentParentList, userUUID, completedAssessment);
		meta = (Map<String, ContentProgressModel>) hierarchy.get("meta");

		List<String> contentIds = new ArrayList<String>((Set<String>) hierarchy.get("progress_id_set"));
		if (!updateOnlyHistory) {

			SearchHits searchHits = ConnectionManager
					.getClient().search(
							new SearchRequest().indices("lexcontentindex").types("resource")
									.searchType(SearchType.QUERY_THEN_FETCH)
									.source(new SearchSourceBuilder().query(QueryBuilders.termsQuery("_id", contentIds))
											.fetchSource(new String[] { "identifier", "resourceType", "duration" },
													new String[0])
											.size(contentIds.size())),
							RequestOptions.DEFAULT)
					.getHits();
			for (SearchHit searchHit : searchHits) {
				Map<String, Object> source = searchHit.getSourceAsMap();
				contentProgressMap.put(source.get("identifier").toString(), new ContentProgress(
						source.get("resourceType").toString(), -1, Long.parseLong(source.get("duration").toString())));
			}

			List<ContentProgressModel> contentProgressList = contentProgressRepo.getProgress(rootOrg,userUUID,
					new ArrayList<String>((Set<String>) hierarchy.get("content_type_set")), contentIds);

			for (ContentProgressModel cpm : contentProgressList) {
				contentProgressMap.get(cpm.getPrimaryKey().getContentId()).setProgress(cpm.getProgress());
			}
		}
		ret.put("parent", hierarchy.getOrDefault("parent", ""));
		ret.put("meta", meta);
		ret.put("content_progress", contentProgressMap);
		return ret;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getHierarchyForResource(String rootOrg,List<String> contentParentList, String userUUID,
			Boolean completedAssessment) {
		Map<String, Object> ret = new HashMap<>();
		try {
			// content meta to be updates
			Map<String, ContentProgressModel> contentDataMap = new HashMap<>();
			// set of ids for fetching cassandra progress
			Set<String> contentIDSet = new HashSet<String>();
			// a set to avoid repeated searches
			Set<String> parentIDSet = new HashSet<String>();
			// a set for content types for the query
			Set<String> contentTypeSet = new HashSet<String>();
			contentTypeSet.add("Resource");
			// a set of ids to get meta for
			Set<String> searchSet = new HashSet<String>();

			// adding parents for the resource
			searchSet.addAll(contentParentList);
			contentIDSet.addAll(contentParentList);

			while (!searchSet.isEmpty()) {
				Set<String> nextSearchSet = new HashSet<String>();

				SearchHits searchHits = ConnectionManager.getClient().search(new SearchRequest()
						.indices("lexcontentindex").types("resource").searchType(SearchType.QUERY_THEN_FETCH)
						.source(new SearchSourceBuilder().query(QueryBuilders.termsQuery("_id", searchSet))
								.fetchSource(new String[] { "identifier", "collections.identifier",
										"children.identifier", "contentType" }, new String[0])
								.size(searchSet.size())),
						RequestOptions.DEFAULT).getHits();

				for (SearchHit searchHit : searchHits) {
					Map<String, Object> source = searchHit.getSourceAsMap();
					ContentProgressPrimaryKeyModel primaryKey = new ContentProgressPrimaryKeyModel(rootOrg,userUUID,
							source.get("contentType").toString(), source.get("identifier").toString());
					ContentProgressModel contentData = new ContentProgressModel(primaryKey);

					contentTypeSet.add(source.get("contentType").toString());

					List<String> childrenList = new ArrayList<>();
					if (source.containsKey("children")) {
						for (Map<String, Object> child : ((List<Map<String, Object>>) source.get("children"))) {
							childrenList.add(child.get("identifier").toString());
						}
					}
					contentData.setChildrenList(childrenList);
					contentIDSet.addAll(childrenList);

					List<String> parentList = new ArrayList<>();
					if (source.containsKey("collections")) {
						for (Map<String, Object> parent : ((List<Map<String, Object>>) source.get("collections"))) {
							parentList.add(parent.get("identifier").toString());
							if (!parentIDSet.contains(parent.get("identifier").toString()))
								nextSearchSet.add(parent.get("identifier").toString());
						}
					}
					contentData.setParentList(parentList);
					contentIDSet.addAll(parentList);
					parentIDSet.addAll(parentList);
					// added meta
					contentDataMap.put(source.get("identifier").toString(), contentData);
					if (completedAssessment)
						if (!ret.containsKey("parent"))
							ret.put("parent", source.get("contentType").toString());
					if (ret.get("parent").toString().toLowerCase().equals("collection")
							&& source.get("contentType").toString().toLowerCase().equals("course"))
						ret.put("parent", source.get("contentType").toString());
				}
				// set false to find the parent for points at the first level only
				completedAssessment = false;
				searchSet = nextSearchSet;
			}
			ret.put("meta", contentDataMap);
			ret.put("progress_id_set", contentIDSet);
			ret.put("content_type_set", contentTypeSet);
		} catch (Exception e) {
			ProjectLogger.log("Error : " + e.getMessage(), e);
		}
		return ret;
	}

	// get all required meta for resource and validates if the id exists
	@SuppressWarnings("unchecked")
	private Map<String, Object> getMetaForResource(String rootOrg,String contentId, String userUUID) throws Exception {
		Map<String, Object> ret = new HashMap<>();
		try {
			Map<String, ContentProgressModel> contentDataMap = new HashMap<>();

			SearchHits searchHits = ConnectionManager.getClient().search(
					new SearchRequest().indices("lexcontentindex").types("resource")
							.searchType(SearchType.QUERY_THEN_FETCH)
							.source(new SearchSourceBuilder().query(QueryBuilders.termsQuery("_id", contentId))
									.fetchSource(new String[] { "identifier", "collections.identifier", "resourceType",
											"contentType", "duration" }, new String[0])
									.size(1)),
					RequestOptions.DEFAULT).getHits();

			if (searchHits.totalHits != 0) {
				Map<String, Object> source = searchHits.getAt(0).getSourceAsMap();
				ContentProgressPrimaryKeyModel primaryKey = new ContentProgressPrimaryKeyModel(rootOrg,userUUID,
						source.get("contentType").toString(), source.get("identifier").toString());
				ContentProgressModel contentData = new ContentProgressModel(primaryKey);
				List<String> parentList = new ArrayList<>();
				if (source.containsKey("collections")) {
					for (Map<String, Object> parent : ((List<Map<String, Object>>) source.get("collections"))) {
						parentList.add(parent.get("identifier").toString());
					}
				}
				contentData.setParentList(parentList);
				contentDataMap.put(source.get("identifier").toString(), contentData);
				ret.put("duration", source.get("duration").toString());
				ret.put("resource_type", source.get("resourceType").toString());
			} else
				throw new InvalidDataInputException("Invalid Resource ID");
			ret.put("meta", contentDataMap);
		} catch (InvalidDataInputException e) {
			ProjectLogger.log("Error : " + e.getMessage(), e);
			throw e;
		} catch (Exception e) {
			ProjectLogger.log("Error : " + e.getMessage(), e);
			throw e;
		}
		return ret;
	}

}
