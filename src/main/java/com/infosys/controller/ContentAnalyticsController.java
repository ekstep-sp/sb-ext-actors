package com.infosys.controller;

import com.infosys.service.ContentAnalyticsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sunbird.common.Constants;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.responsecode.ResponseCode;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class ContentAnalyticsController {

	private final ContentAnalyticsService contentAnalyticsService;

	public ContentAnalyticsController(ContentAnalyticsService contentAnalyticsService) {
		this.contentAnalyticsService = contentAnalyticsService;
	}

	/**
	 * @param rootOrg Name of root Organization
	 * @param org Name of Organization inside rootOrg
	 * @param langCode e.g. en,ar,es
	 * @param contentStatus Draft,Reviewed,Live
	 * @param startDate Optional format = yyyy-MM-dd HH:mm:ss timeZone = GMT
	 * @param endDate Optional format = yyyy-MM-dd HH:mm:ss timeZone = GMT
	 * @return Response model of  ount of contentType Resource,Collection,Course etc based on status, date filters
	 */
	@GetMapping("/v1/content/{langCode}/{contentStatus}/count")
	public ResponseEntity<Response> getContentCountStats(@RequestHeader("rootOrg") String rootOrg,
														@RequestHeader("org") String org,
														@PathVariable("langCode") String langCode,
														@PathVariable("contentStatus") String contentStatus,
														@RequestParam(value = "startDate", required = false) Timestamp startDate,
														@RequestParam(value = "endDate", required = false) Timestamp endDate) {
		HttpStatus status;
		Response resp = new Response();
		resp.setVer("v1");
		resp.setId("api.getContentCountStats");
		resp.setTs(ProjectUtil.getFormattedDate());
		try {
			// Parsing in format required by ElasticSearch
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat(com.infosys.util.Constants.BASIC_DATE_TIME_NO_MILLIS);
			String sDate = startDate == null ? null : simpleDateFormat.format(startDate);
			String eDate = endDate == null ? null : simpleDateFormat.format(endDate);
			Map<String, Object> result = contentAnalyticsService.getContentCountByStatus(rootOrg, org, langCode, contentStatus, sDate, eDate);
			status = result.isEmpty() ? HttpStatus.NO_CONTENT : HttpStatus.OK;
			resp.put(Constants.RESPONSE, result);
		} catch (Exception e) {
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.internalError.getErrorCode());
			resp.put("Error", e.getMessage());
			resp.setResponseCode(responseCode);
			status = HttpStatus.INTERNAL_SERVER_ERROR;
		}
		return new ResponseEntity<>(resp, status);
	}
}
