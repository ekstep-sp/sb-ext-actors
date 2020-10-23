package com.infosys.service;

import java.io.IOException;
import java.util.Map;

public interface ContentAnalyticsService {
	Map<String, Object> getContentCountByStatus(String rootOrg, String org, String langCode, String contentStatus, String startDate, String endDate) throws IOException;
}
