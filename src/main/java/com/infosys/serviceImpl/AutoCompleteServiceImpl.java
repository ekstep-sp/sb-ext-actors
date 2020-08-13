/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion.Entry.Option;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.responsecode.ResponseCode;

import com.infosys.elastic.helper.ConnectionManager;
import com.infosys.service.AutoCompleteService;
import com.infosys.service.UserUtilityService;
import com.infosys.util.LexProjectUtil;

@Service
public class AutoCompleteServiceImpl implements AutoCompleteService {

	@Autowired
	UserUtilityService userUtilService;

	@Override
	public Response getSuggestionsForQuery(String prefix, String field) {

		Response contentResponse = new Response();

		try {
			RestHighLevelClient requestBuilder = ConnectionManager.getClient();

			CompletionSuggestionBuilder autoSuggest = new CompletionSuggestionBuilder(field);
			// autoSuggest = autoSuggest.prefix(prefix);
			autoSuggest.text(prefix);

			SuggestBuilder suggestBuilder = new SuggestBuilder();
			suggestBuilder.addSuggestion("autocomplete_suggestion", autoSuggest);

			SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
			searchSourceBuilder.suggest(suggestBuilder);

			SearchResponse searchResponse = requestBuilder
					.search(new SearchRequest().indices(LexProjectUtil.EsIndex.lexTopic.getIndexName())
							.types(LexProjectUtil.EsType.content.getTypeName()).searchType(SearchType.QUERY_THEN_FETCH)
							.source(searchSourceBuilder), RequestOptions.DEFAULT);

			CompletionSuggestion suggestions = searchResponse.getSuggest().getSuggestion("autocomplete_suggestion");

			List<Option> optionList = suggestions.getOptions();
			List<String> suggestionList = new ArrayList<String>();

			for (Option option : optionList) {
				Map<String, Object> map = option.getHit().getSourceAsMap();
				suggestionList.add((String) map.get("name"));
			}

			// System.out.println(suggestions.get("text"));
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
			responseCode.setResponseCode(ResponseCode.OK.getResponseCode());
			contentResponse.setResponseCode(responseCode);
			contentResponse.put("suggestions", suggestionList);

		} catch (NoNodeAvailableException e) {
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.unableToConnectToES.getErrorCode());
			responseCode.setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR);
			contentResponse.setResponseCode(responseCode);
		} catch (IOException e) {
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.unableToConnectToES.getErrorCode());
			responseCode.setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR);
			contentResponse.setResponseCode(responseCode);
		}

		return contentResponse;
	}

	@Override
	public List<Map<String, Object>> getSearchData(String searchString) throws Exception {
		List<Map<String, Object>> userList = new ArrayList<>();
		if (userUtilService.getValidationOptions().toLowerCase().contains("graph"))
			userList = userUtilService.getSearchDataFromActiveDirectory(searchString);
		if (userList.size() != 0) {
			for (Map<String, Object> user : userList) {
				String temp = user.get("companyName") == null ? "" : user.get("companyName").toString();
				user.put("id", temp);
				user.remove("companyName");

				temp = user.get("displayName") == null ? "" : user.get("displayName").toString();
				user.put("displayName", temp);

				temp = user.get("mail") == null ? "" : user.get("mail").toString();
				user.put("mail", temp);
			}
		} else {
			userList = userUtilService.getUserSuggestionsForQuery(searchString);
		}
		return userList;
	}
}
