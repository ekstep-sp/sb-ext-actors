package com.infosys.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface ContentAnalyticsService {
	Map<String, Object> getContentCountByStatus(String rootOrg, String org, String langCode, String contentStatus, String startDate, String endDate) throws IOException;

	List<Map<String, Object>> getExternalResourcesList(String rootOrg, String org, String langCode, String startDate, String endDate, int searchSize, int offSet, String[] includeFields,String search_query) throws IOException;
}
