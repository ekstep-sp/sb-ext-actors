package com.infosys.service;

import java.util.List;
import java.util.Map;

public interface PostAnalyticsService {
    List<Map<String, Object>> getPostAnalyticContent(String rootOrg, String org, String postKind, String postStatus, String startDate, String endDate, String[] includeFields) throws Exception;
}
