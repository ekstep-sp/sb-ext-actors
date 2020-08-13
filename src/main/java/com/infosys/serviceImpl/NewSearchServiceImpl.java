/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder.Type;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.InternalFilter;
import org.elasticsearch.search.aggregations.bucket.nested.InternalNested;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

import com.infosys.elastic.helper.ConnectionManager;
import com.infosys.model.FilterClass;
import com.infosys.model.FilterItem;
import com.infosys.service.NewSearchService;
import com.infosys.util.LexProjectUtil;

@Service
public class NewSearchServiceImpl implements NewSearchService {

	public static void addFilters(List<Map<String, Object>> completeMap, SearchResponse searchResponse,
			TermsAggregationBuilder aggBuilder, String aggregationName, String filterName, String displayName) {
		if (aggBuilder != null) {
			List<FilterItem> filterItemList = new ArrayList<FilterItem>();
			Terms aggregation = searchResponse.getAggregations().get(aggregationName);
			for (Terms.Bucket bucket : aggregation.getBuckets()) {
				FilterItem filterItem = new FilterItem();
				if (bucket.getKeyAsString() == null || bucket.getKeyAsString().isEmpty()) {

				} else if (bucket.getDocCount() > 0) {
					filterItem.setType(bucket.getKeyAsString());
					filterItem.setDisplayName(bucket.getKeyAsString());
					filterItem.setCount(bucket.getDocCount());
					filterItemList.add(filterItem);
				}

			}
			Map<String, Object> topicAggMap = new HashMap<String, Object>();
			topicAggMap.put("type", filterName);
			topicAggMap.put("displayName", displayName);
			topicAggMap.put("content", filterItemList);
			completeMap.add(topicAggMap);

		}
	}

	@Override
	public Response searchSearvice(Request request) throws IOException {
		Response response = new Response();
		response.setTs(ProjectUtil.getFormattedDate());
		Map<String, Object> requestMap = request.getRequest();
		Map<String, Object> resultResponse = new HashMap<String, Object>();
		String query = null;
		String[] contentType = null;
		String[] resourceType = null;
		String[] topics = null;
		String[] tracks = null;
		String[] subTracks = null;
		String[] subSubTracks = null;
		String[] subSubSubTracks = null;
		String[] categories = null;
		String[] resourceCategories = null;
		String[] mimeTypes = null;
		String[] sourceNames = null;
		Integer pageNumber = null;
		Integer pageSize = null;
		String sortBy = null;
		String sortOrder = null;
		boolean sortFlag = false;
		boolean invalidFlag = false;
		try {
			query = (String) requestMap.get("query");

			Map<String, Object> filterMap = (Map<String, Object>) requestMap.get("filters");

			if (filterMap != null) {
				FilterClass filters = FilterClass.fromMap(filterMap);
				contentType = filters.getContentType();
				resourceType = filters.getResourceType();
				topics = filters.getConcepts();
				tracks = filters.getTracks();
				subTracks = filters.getSubTracks();
				subSubTracks = filters.getSubSubTracks();
				subSubSubTracks = filters.getSubSubSubTracks();
				categories = filters.getCategories();
				resourceCategories = filters.getResourceCategory();
				mimeTypes = filters.getMimeType();
				sourceNames = filters.getSourceName();
			}

			if (contentType == null && resourceType == null && topics == null && tracks == null && query == null
					&& subTracks == null && subSubTracks == null && subSubSubTracks == null
					&& resourceCategories == null && sourceNames == null)
				invalidFlag = true;

			pageNumber = (Integer) requestMap.get("pageNo");
			pageSize = (Integer) requestMap.get("pageSize");
			sortBy = (String) requestMap.get("sortBy");
			sortOrder = (String) requestMap.get("sortOrder");

			if (pageNumber == null)
				pageNumber = 0;
			if (pageSize == null)
				pageSize = 10;
			if (sortBy != null)
				sortFlag = true;
			if (sortOrder == null || !sortOrder.equals("ASC"))
				sortOrder = "DESC";
		} catch (Exception e) {
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
			response.setResponseCode(responseCode);
			e.printStackTrace();
			return response;
		}

		if (invalidFlag == true) {
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
			response.setResponseCode(responseCode);
			return response;
		}

		RestHighLevelClient requestBuilder = ConnectionManager.getClient();

		BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();

		TermsAggregationBuilder contentAggregationBuilder = null;
		TermsAggregationBuilder resourceAggregationBuilder = null;
		TermsAggregationBuilder trackAggregationBuilder = null;
		TermsAggregationBuilder topicAggregationBuilder = null;
		TermsAggregationBuilder resourceCategoriesAggregationBuilder = null;
		TermsAggregationBuilder mimeTypesAggregationBuilder = null;
		TermsAggregationBuilder sourceNamesAggregationBuilder = null;

		boolean nonEmptyFlag = false;

		if (query != null && query.toLowerCase().equals("all"))
			query = "";

		if (query != null && !query.isEmpty()) {
			if (!query.equals("\"") && !query.equals("\"\"") && query.charAt(0) == '"'
					&& query.charAt(query.length() - 1) == '"') {
				query = query.substring(1, query.length());
				queryBuilder
						.must(QueryBuilders
								.multiMatchQuery(query, "identifier", "name", "description", "tags", "keywords",
										"skills", "creatorContacts", "concepts", "domain", "resourceCategory",
										"resourceType", "mimeType", "unit", "sourceName", "sourceShortName")
								.type(Type.PHRASE));
				// System.out.println(query + " in \"\"");
			} else {
				queryBuilder
						.must(QueryBuilders
								.multiMatchQuery(query, "identifier", "name", "description", "tags", "keywords",
										"skills", "creatorContacts", "concepts", "domain", "resourceCategory",
										"resourceType", "mimeType", "unit", "sourceName", "sourceShortName")
								.type(Type.MOST_FIELDS));
				nonEmptyFlag = true;
				// System.out.println("not in that");
			}
			nonEmptyFlag = true;
		}
		if (contentType != null && contentType.length > 0) {
			searchRequestBuilder(queryBuilder, contentType, "contentType", false);
			nonEmptyFlag = true;
		}

		if (resourceType != null && resourceType.length > 0) {
			searchRequestBuilder(queryBuilder, resourceType, "resourceType", false);
			nonEmptyFlag = true;
		}

		if (sourceNames != null && sourceNames.length > 0) {
			searchRequestBuilder(queryBuilder, sourceNames, "sourceName", false);
			nonEmptyFlag = true;
		}

		if (resourceCategories != null && resourceCategories.length > 0) {
			searchRequestBuilder(queryBuilder, resourceCategories, "resourceCategory", false);
			nonEmptyFlag = true;
		}

		if (mimeTypes != null && mimeTypes.length > 0) {
			searchRequestBuilder(queryBuilder, mimeTypes, "mimeType", false);
			nonEmptyFlag = true;
		}

		/*
		 * if (resourceCategories!= null && resourceCategories.length > 0) {
		 * searchRequestBuilder(queryBuilder, resourceCategories, "resourceCategory",
		 * false); nonEmptyFlag = true; }
		 */

		if (tracks != null && tracks.length > 0) {
			searchRequestBuilder(queryBuilder, tracks, "track.name", false);
			nonEmptyFlag = true;
		}

		if (topics != null && topics.length > 0) {
			searchRequestBuilder(queryBuilder, topics, "concepts.name", false);
			nonEmptyFlag = true;
		}
		if (subTracks != null && subTracks.length > 0) {
			searchRequestBuilder(queryBuilder, subTracks, "", true);
			nonEmptyFlag = true;
		}

		if (subSubTracks != null && subSubTracks.length > 0) {
			searchRequestBuilder(queryBuilder, subSubTracks, "", true);
			nonEmptyFlag = true;
		}

		if (subSubSubTracks != null && subSubSubTracks.length > 0) {
			searchRequestBuilder(queryBuilder, subSubSubTracks, "", true);
			nonEmptyFlag = true;
		}

		if (categories != null && categories.length > 0) {
			searchRequestBuilder(queryBuilder, categories, "", true);
			nonEmptyFlag = true;
		}

		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		if (nonEmptyFlag == true) {
			contentAggregationBuilder = AggregationBuilders.terms("content_aggs").field("contentType.keyword")
					.size(1000);
			sourceBuilder.aggregation(contentAggregationBuilder);

			resourceAggregationBuilder = AggregationBuilders.terms("resource_aggs").field("resourceType.keyword")
					.size(1000);
			sourceBuilder.aggregation(resourceAggregationBuilder);

			trackAggregationBuilder = AggregationBuilders.terms("track_aggs").field("track.name.keyword").size(1000);
			sourceBuilder.aggregation(trackAggregationBuilder);

			resourceCategoriesAggregationBuilder = AggregationBuilders.terms("resourceCategory_aggs")
					.field("resourceCategory.keyword").size(1000);
			sourceBuilder.aggregation(resourceCategoriesAggregationBuilder);

			mimeTypesAggregationBuilder = AggregationBuilders.terms("mimeType_aggs").field("mimeType.keyword")
					.size(1000);
			sourceBuilder.aggregation(mimeTypesAggregationBuilder);

			sourceNamesAggregationBuilder = AggregationBuilders.terms("sourceName_aggs").field("sourceName.keyword")
					.size(1000);
			sourceBuilder.aggregation(sourceNamesAggregationBuilder);

			topicAggregationBuilder = AggregationBuilders.terms("topic_aggs").field("concepts.name.keyword").size(1000);
			sourceBuilder.aggregation(topicAggregationBuilder);

			AggregationBuilder subTrackAggregationBuilder = AggregationBuilders.nested("subTracks_aggs", "tags")
					.subAggregation(AggregationBuilders
							.filter("tags_1", QueryBuilders.matchQuery("tags.type.keyword", "SubTrack")).subAggregation(
									AggregationBuilders.terms("tags_2").field("tags.value.keyword").size(1000)));
			sourceBuilder.aggregation(subTrackAggregationBuilder);

			AggregationBuilder subSubTrackAggregationBuilder = AggregationBuilders.nested("subSubTracks_aggs", "tags")
					.subAggregation(AggregationBuilders
							.filter("tags_1", QueryBuilders.matchQuery("tags.type.keyword", "SubSubTrack"))
							.subAggregation(
									AggregationBuilders.terms("tags_2").field("tags.value.keyword").size(1000)));
			sourceBuilder.aggregation(subSubTrackAggregationBuilder);

			AggregationBuilder subSubSubTrackAggregationBuilder = AggregationBuilders
					.nested("subSubSubTracks_aggs", "tags")
					.subAggregation(AggregationBuilders
							.filter("tags_1", QueryBuilders.matchQuery("tags.type.keyword", "SubSubSubTrack"))
							.subAggregation(
									AggregationBuilders.terms("tags_2").field("tags.value.keyword").size(1000)));
			sourceBuilder.aggregation(subSubSubTrackAggregationBuilder);

			AggregationBuilder categoryAggregationBuilder = AggregationBuilders.nested("categories_aggs", "tags")
					.subAggregation(AggregationBuilders
							.filter("tags_1", QueryBuilders.matchQuery("tags.type", "Category")).subAggregation(
									AggregationBuilders.terms("tags_2").field("tags.value.keyword").size(1000)));
			sourceBuilder.aggregation(categoryAggregationBuilder);

			if (sortBy == null)
				;
			else {
				// System.out.println(sortBy);
				if (sortOrder.equals("ASC")) {
					sourceBuilder.sort(sortBy, SortOrder.ASC);
					// System.out.println("added sort ascending");
				} else {
					sourceBuilder.sort(sortBy, SortOrder.DESC);
					// System.out.println("added sort descending");
				}
			}
			sourceBuilder.query(queryBuilder).size(pageSize).from(pageNumber * pageSize);
			SearchResponse searchResponse = requestBuilder.search(
					new SearchRequest().indices(LexProjectUtil.EsIndex.bodhi.getIndexName())
							.types(LexProjectUtil.EsType.resource.getTypeName())
							.searchType(SearchType.DFS_QUERY_THEN_FETCH).source(sourceBuilder).source(sourceBuilder),
					RequestOptions.DEFAULT);

			List<Map<String, Object>> completeMap = new ArrayList<Map<String, Object>>();

			Map<String, List<String>> aggAndFilterNamesMap = new HashMap<String, List<String>>();
			Map<String, List<String>> contentAggsNamesMap = new HashMap<String, List<String>>();
			List<String> contentTypeList = new ArrayList<String>();
			List<String> resourceTypeList = new ArrayList<String>();
			List<String> topicList = new ArrayList<String>();
			List<String> trackList = new ArrayList<String>();
			List<String> resourceCategoryList = new ArrayList<String>();
			List<String> mimeTypeList = new ArrayList<String>();
			List<String> sourceNameList = new ArrayList<String>();

			contentTypeList.add("contentType");
			contentTypeList.add("Content-Type");

			resourceTypeList.add("resourceType");
			resourceTypeList.add("Resource Type");

			topicList.add("concepts");
			topicList.add("Concepts");

			trackList.add("tracks");
			trackList.add("Tracks");

			resourceCategoryList.add("resourceCategory");
			resourceCategoryList.add("Resource Categories");

			mimeTypeList.add("mimeType");
			mimeTypeList.add("Mime Types");

			sourceNameList.add("sourceName");
			sourceNameList.add("Sources");

			aggAndFilterNamesMap.put("content_aggs", contentTypeList);
			aggAndFilterNamesMap.put("resource_aggs", resourceTypeList);
			aggAndFilterNamesMap.put("track_aggs", trackList);
			aggAndFilterNamesMap.put("topic_aggs", topicList);
			aggAndFilterNamesMap.put("resourceCategory_aggs", resourceCategoryList);
			aggAndFilterNamesMap.put("mimeType_aggs", mimeTypeList);
			aggAndFilterNamesMap.put("sourceName_aggs", sourceNameList);

			List<Map<String, Object>> contentMap = new ArrayList<Map<String, Object>>();

			Iterator<Map.Entry<String, List<String>>> aggAndFilterNamesItr = aggAndFilterNamesMap.entrySet().iterator();
			while (aggAndFilterNamesItr.hasNext()) {
				Map.Entry<String, List<String>> pair = (Map.Entry<String, List<String>>) aggAndFilterNamesItr.next();
				addFilters(completeMap, searchResponse, contentAggregationBuilder, pair.getKey().toString().trim(),
						pair.getValue().get(0).toString().trim(), pair.getValue().get(1).toString().trim());
			}

			InternalNested subTrackAggregationNested = searchResponse.getAggregations().get("subTracks_aggs");
			Map<String, Aggregation> internalSubTrackAggregations = subTrackAggregationNested.getAggregations().asMap();
			InternalFilter internalFilterSubTrack = (InternalFilter) internalSubTrackAggregations.get("tags_1");
			// Map<String,Aggregation>map =internalFilterSubTrack.getAggregations().asMap();

			StringTerms stringTermsSubTrack = internalFilterSubTrack.getAggregations().get("tags_2");

			List<FilterItem> subTrackList = new ArrayList<FilterItem>();
			for (Terms.Bucket bucket : stringTermsSubTrack.getBuckets()) {
				FilterItem filterItem = new FilterItem();
				if (bucket.getKeyAsString().isEmpty()) {

				} else {
					filterItem.setType(bucket.getKeyAsString());
					filterItem.setDisplayName(bucket.getKeyAsString());
					filterItem.setCount(bucket.getDocCount());
					subTrackList.add(filterItem);
				}

			}
			Map<String, Object> subTrackAggMap = new HashMap<String, Object>();
			subTrackAggMap.put("type", "subTracks");
			subTrackAggMap.put("displayName", "Sub Tracks");
			subTrackAggMap.put("content", subTrackList);
			completeMap.add(subTrackAggMap);

			completeMap.add(subSubTrackAggregation(searchResponse, "subSubTracks", "Topics"));
			completeMap.add(subSubTrackAggregation(searchResponse, "subSubSubTracks", "Sub Topics"));

			InternalNested categoryAggregationNested = searchResponse.getAggregations().get("categories_aggs");
			Map<String, Aggregation> categoriesAggregations = categoryAggregationNested.getAggregations().asMap();
			InternalFilter internalFiltercategory = (InternalFilter) categoriesAggregations.get("tags_1");
			StringTerms stringTermscategories = internalFiltercategory.getAggregations().get("tags_2");

			List<FilterItem> categoryList = new ArrayList<FilterItem>();
			for (Terms.Bucket bucket : stringTermscategories.getBuckets()) {
				FilterItem filterItem = new FilterItem();
				if (bucket.getKeyAsString().isEmpty()) {

				} else {
					filterItem.setType(bucket.getKeyAsString());
					filterItem.setDisplayName(bucket.getKeyAsString());
					filterItem.setCount(bucket.getDocCount());
					categoryList.add(filterItem);
				}
			}
			Map<String, Object> categoryAggMap = new HashMap<String, Object>();
			categoryAggMap.put("type", "categories");
			categoryAggMap.put("displayName", "Categories");
			categoryAggMap.put("content", categoryList);
			completeMap.add(categoryAggMap);

			List<Object> intersectionMetaList = new ArrayList<Object>();
			for (SearchHit hit : searchResponse.getHits()) {

				Map<String, Object> map = hit.getSourceAsMap();
				intersectionMetaList.add(map);

			}

			Map<String, Object> result = new HashMap<String, Object>();
			result.put("totalHits", searchResponse.getHits().totalHits);
			result.put("result", intersectionMetaList);
			result.put("filters", completeMap);
			response.put("response", result);
			return response;
		} else {
			MatchAllQueryBuilder matchAllQueryBuilder = QueryBuilders.matchAllQuery();
			if (sortBy == null) {

			} else {
				// System.out.println(sortBy);
				if (sortOrder.equals("ASC")) {
					// System.out.println("added sort ascending");
					sourceBuilder.sort(sortBy, SortOrder.ASC);
				} else {
					// System.out.println("added sort descending");
					sourceBuilder.sort(sortBy, SortOrder.DESC);
				}
			}
			
			sourceBuilder.query(matchAllQueryBuilder).size(pageSize).from(pageNumber * pageSize);
			SearchResponse searchResponse = requestBuilder.search(
					new SearchRequest().indices(LexProjectUtil.EsIndex.bodhi.getIndexName())
							.types(LexProjectUtil.EsType.resource.getTypeName())
							.searchType(SearchType.DFS_QUERY_THEN_FETCH).source(sourceBuilder).source(sourceBuilder),
					RequestOptions.DEFAULT);
			
			List<Object> allEntries = new ArrayList<Object>();

			// System.out.println(searchResponse.getHits().totalHits);

			for (SearchHit hit : searchResponse.getHits()) {
				Map<String, Object> map = hit.getSourceAsMap();
				allEntries.add(map);
			}
			List uniqueFieldValues = getUniqueValuesOfFieldsWithCount2();
			resultResponse.put("totalHits", searchResponse.getHits().totalHits);
			resultResponse.put("result", allEntries);
			resultResponse.put("filters", uniqueFieldValues);
			response.put("response", resultResponse);
			return response;
		}
	}

	public List<Map<String, Object>> getUniqueValuesOfFieldsWithCount2() throws IOException {
		RestHighLevelClient requestBuilder = ConnectionManager.getClient();

		int scrollSize = 1000;
		int i = 0;

		List<Map<String, Map<String, Long>>> returnList = new ArrayList<Map<String, Map<String, Long>>>();

		AggregationBuilder conceptsAggregationBuilder = AggregationBuilders.terms("concepts_aggs")
				.field("concepts.name.keyword").size(1000);
		AggregationBuilder trackAggregationBuilder = AggregationBuilders.terms("track_aggs").field("track.name.keyword")
				.size(1000);
		AggregationBuilder contentTypeAggregationBuilder = AggregationBuilders.terms("contentType_aggs")
				.field("contentType.keyword").size(1000);
		AggregationBuilder resourceCategoryAggregationBuilder = AggregationBuilders.terms("resourceCategory_aggs")
				.field("resourceCategory.keyword").size(1000);
		AggregationBuilder mimeTypeAggregationBuilder = AggregationBuilders.terms("mimeType_aggs")
				.field("mimeType.keyword").size(1000);
		AggregationBuilder sourceNameAggregationBuilder = AggregationBuilders.terms("sourceName_aggs")
				.field("sourceName.keyword").size(1000);
		AggregationBuilder resourceTypeAggregationBuilder = AggregationBuilders.terms("resourceType_aggs")
				.field("resourceType.keyword").size(1000);
		AggregationBuilder subTrackAggregationBuilder = AggregationBuilders.nested("subTracks_aggs", "tags")
				.subAggregation(AggregationBuilders
						.filter("tags_1", QueryBuilders.matchQuery("tags.type.keyword", "SubTrack"))
						.subAggregation(AggregationBuilders.terms("tags_2").field("tags.value.keyword").size(1000)));
		AggregationBuilder subSubTrackAggregationBuilder = AggregationBuilders.nested("subSubTracks_aggs", "tags")
				.subAggregation(AggregationBuilders
						.filter("tags_1", QueryBuilders.matchQuery("tags.type.keyword", "SubSubTrack"))
						.subAggregation(AggregationBuilders.terms("tags_2").field("tags.value.keyword").size(1000)));
		AggregationBuilder subSubSubTrackAggregationBuilder = AggregationBuilders.nested("subSubSubTracks_aggs", "tags")
				.subAggregation(AggregationBuilders
						.filter("tags_1", QueryBuilders.matchQuery("tags.type.keyword", "SubSubSubTrack"))
						.subAggregation(AggregationBuilders.terms("tags_2").field("tags.value.keyword").size(1000)));
		AggregationBuilder categoriesAggregationBuilder = AggregationBuilders.nested("categories_aggs", "tags")
				.subAggregation(AggregationBuilders
						.filter("tags_1", QueryBuilders.matchQuery("tags.type.keyword", "Category"))
						.subAggregation(AggregationBuilders.terms("tags_2").field("tags.value.keyword").size(1000)));

		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.aggregation(conceptsAggregationBuilder);
		sourceBuilder.aggregation(trackAggregationBuilder);
		sourceBuilder.aggregation(contentTypeAggregationBuilder);
		sourceBuilder.aggregation(resourceTypeAggregationBuilder);
		sourceBuilder.aggregation(subTrackAggregationBuilder);
		sourceBuilder.aggregation(subSubTrackAggregationBuilder);
		sourceBuilder.aggregation(subSubSubTrackAggregationBuilder);
		sourceBuilder.aggregation(categoriesAggregationBuilder);
		sourceBuilder.aggregation(resourceCategoryAggregationBuilder);
		sourceBuilder.aggregation(mimeTypeAggregationBuilder);
		sourceBuilder.aggregation(sourceNameAggregationBuilder);
		SearchResponse response = requestBuilder
				.search(new SearchRequest().indices(LexProjectUtil.EsIndex.bodhi.getIndexName())
						.types(LexProjectUtil.EsType.resource.getTypeName()).searchType(SearchType.DFS_QUERY_THEN_FETCH)
						.source(sourceBuilder), RequestOptions.DEFAULT);

		List<Map<String, Object>> completeMap = new ArrayList<Map<String, Object>>();

		Map<String, Object> topicAggMap = addAggregationToResponse(response, "concepts_aggs", "concepts", "Concepts");
		completeMap.add(topicAggMap);

		Map<String, Object> trackAggMap = addAggregationToResponse(response, "track_aggs", "tracks", "Tracks");
		completeMap.add(trackAggMap);

		Map<String, Object> contentTypeAggMap = addAggregationToResponse(response, "contentType_aggs", "contentType",
				"Content-Type");

		Map<String, Object> resourceCategoryAggMap = addAggregationToResponse(response, "resourceCategory_aggs",
				"resourceCategory", "Resource Categories");
		completeMap.add(resourceCategoryAggMap);

		Map<String, Object> mimeTypeAggMap = addAggregationToResponse(response, "mimeType_aggs", "mimeType",
				"Mime Types");
		completeMap.add(mimeTypeAggMap);

		Map<String, Object> sourceNameAggMap = addAggregationToResponse(response, "sourceName_aggs", "sourceName",
				"Sources");
		completeMap.add(sourceNameAggMap);

		Map<String, Object> resourceAggMap = addAggregationToResponse(response, "resourceType_aggs", "resourceType",
				"Resource Types");
		completeMap.add(resourceAggMap);

		InternalNested subTrackAggregationNested = response.getAggregations().get("subTracks_aggs");
		Map<String, Aggregation> internalSubTrackAggregations = subTrackAggregationNested.getAggregations().asMap();
		InternalFilter internalFilterSubTrack = (InternalFilter) internalSubTrackAggregations.get("tags_1");

		StringTerms stringTermsSubTrack = internalFilterSubTrack.getAggregations().get("tags_2");

		List<FilterItem> subTrackList = new ArrayList<FilterItem>();
		for (Terms.Bucket bucket : stringTermsSubTrack.getBuckets()) {
			FilterItem filterItem = new FilterItem();
			if (bucket.getKeyAsString() == null || bucket.getKeyAsString().isEmpty()) {

			} else if (bucket.getDocCount() > 0) {
				filterItem.setType(bucket.getKeyAsString());
				filterItem.setDisplayName(bucket.getKeyAsString());
				filterItem.setCount(bucket.getDocCount());
				subTrackList.add(filterItem);
			}

		}
		Map<String, Object> subTrackAggMap = new HashMap<String, Object>();
		subTrackAggMap.put("type", "subTracks");
		subTrackAggMap.put("displayName", "Sub Tracks");
		subTrackAggMap.put("content", subTrackList);
		completeMap.add(subTrackAggMap);

		completeMap.add(subSubTrackAggregation(response, "subSubTracks", "Topics"));
		completeMap.add(subSubTrackAggregation(response, "subSubSubTracks", "Sub Topics"));

		InternalNested categoriesAggregationNested = response.getAggregations().get("categories_aggs");
		Map<String, Aggregation> categoriesAggregations = categoriesAggregationNested.getAggregations().asMap();
		InternalFilter internalFiltercategory = (InternalFilter) categoriesAggregations.get("tags_1");

		StringTerms stringTermscategories = internalFiltercategory.getAggregations().get("tags_2");
		List<FilterItem> categoryList = new ArrayList<FilterItem>();
		for (Terms.Bucket bucket : stringTermscategories.getBuckets()) {
			FilterItem filterItem = new FilterItem();
			if (bucket.getKeyAsString() == null || bucket.getKeyAsString().isEmpty()) {

			} else if (bucket.getDocCount() > 0) {
				filterItem.setType(bucket.getKeyAsString());
				filterItem.setDisplayName(bucket.getKeyAsString());
				filterItem.setCount(bucket.getDocCount());
				categoryList.add(filterItem);
			}

		}
		Map<String, Object> categoryAggMap = new HashMap<String, Object>();
		categoryAggMap.put("type", "categories");
		categoryAggMap.put("displayName", "Categories");
		categoryAggMap.put("content", categoryList);
		completeMap.add(categoryAggMap);

		completeMap.add(contentTypeAggMap);

		return completeMap;

	}

	private Map<String, Object> subSubTrackAggregation(SearchResponse response, String field, String displayField) {

		InternalNested subTrackAggregationNested = response.getAggregations().get(field + "_aggs");
		Map<String, Aggregation> internalSubTrackAggregations = subTrackAggregationNested.getAggregations().asMap();
		InternalFilter internalFilterSubTrack = (InternalFilter) internalSubTrackAggregations.get("tags_1");

		StringTerms stringTermsSubTrack = internalFilterSubTrack.getAggregations().get("tags_2");

		List<FilterItem> subTrackList = new ArrayList<FilterItem>();
		for (Terms.Bucket bucket : stringTermsSubTrack.getBuckets()) {
			FilterItem filterItem = new FilterItem();
			if (bucket.getKeyAsString() == null || bucket.getKeyAsString().isEmpty()) {

			} else if (bucket.getDocCount() > 0) {
				filterItem.setType(bucket.getKeyAsString());
				filterItem.setDisplayName(bucket.getKeyAsString());
				filterItem.setCount(bucket.getDocCount());
				subTrackList.add(filterItem);
			}

		}
		Map<String, Object> subTrackAggMap = new HashMap<String, Object>();
		subTrackAggMap.put("type", field);
		subTrackAggMap.put("displayName", displayField);
		subTrackAggMap.put("content", subTrackList);
		return subTrackAggMap;
	}

	public void searchRequestBuilder(BoolQueryBuilder queryBuilder, String[] fieldsArray, String fieldName,
			boolean isNested) {

		BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

		List<String> list = new ArrayList<String>();
		List<String> notList = new ArrayList<String>();
		List<String> mandatoryList = new ArrayList<String>();

		for (int j = 0; fieldsArray != null && j < fieldsArray.length; j++) {
			if (fieldsArray[j].startsWith("not"))
				notList.add(fieldsArray[j].substring(4, fieldsArray[j].length() - 1));
			else if (fieldsArray[j].startsWith("and"))
				mandatoryList.add(fieldsArray[j].substring(4, fieldsArray[j].length() - 1));
			else
				list.add(fieldsArray[j]);
		}

		fieldName += ".keyword";

		for (int j = 0; j < list.size(); j++) {
			if (isNested == false)
				queryBuilder.must(boolQueryBuilder.should(QueryBuilders.termQuery(fieldName, list.get(j))));
			else
				queryBuilder.must(QueryBuilders.nestedQuery("tags",
						boolQueryBuilder.should(QueryBuilders.termQuery("tags.value.keyword", list.get(j))),
						ScoreMode.Max));
			// System.out.println(list.get(j));
		}
		for (int j = 0; j < notList.size(); j++) {
			if (isNested == false)
				queryBuilder.mustNot(QueryBuilders.termQuery(fieldName, notList.get(j)));
			else {
				BoolQueryBuilder boolQueryBuilder2 = QueryBuilders.boolQuery();
				queryBuilder.mustNot(QueryBuilders.nestedQuery("tags",
						boolQueryBuilder2.should(QueryBuilders.termQuery("tags.value.keyword", notList.get(j))),
						ScoreMode.Max));
				// System.out.println(notList.get(j));
			}
		}
		for (int j = 0; j < mandatoryList.size(); j++) {
			BoolQueryBuilder boolQueryBuilder2 = QueryBuilders.boolQuery();
			if (isNested == false)
				queryBuilder.must(QueryBuilders.termQuery(fieldName, mandatoryList.get(j)));
			else {
				queryBuilder.must(QueryBuilders.nestedQuery("tags",
						boolQueryBuilder2.must(QueryBuilders.matchQuery("tags.value", mandatoryList.get(j))),
						ScoreMode.Max));
				// System.out.println("mandatory " + mandatoryList.get(j));
			}
		}
	}

	public Map<String, Object> addAggregationToResponse(SearchResponse response, String aggsName, String type,
			String displayName) {
		Terms filterTypeAggregation = response.getAggregations().get(aggsName);
		List<FilterItem> filterTypeList = new ArrayList<FilterItem>();
		for (Terms.Bucket bucket : filterTypeAggregation.getBuckets()) {
			FilterItem filterItem = new FilterItem();
			if (bucket.getKeyAsString().isEmpty()) {
				filterItem.setType("NA");
				filterItem.setDisplayName("NA");
			} else if (bucket.getDocCount() > 0) {
				filterItem.setType(bucket.getKeyAsString());
				filterItem.setDisplayName(bucket.getKeyAsString());
				filterItem.setCount(bucket.getDocCount());
			}
			filterTypeList.add(filterItem);
		}
		Map<String, Object> filterTypeAggMap = new HashMap<String, Object>();
		filterTypeAggMap.put("type", type);
		filterTypeAggMap.put("displayName", displayName);
		filterTypeAggMap.put("content", filterTypeList);
		return filterTypeAggMap;
	}
}
