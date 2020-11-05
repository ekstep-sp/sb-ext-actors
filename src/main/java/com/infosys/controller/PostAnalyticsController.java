package com.infosys.controller;

import com.infosys.serviceImpl.PostAnalyticsServiceImpl;
import com.infosys.util.LexProjectUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sunbird.common.Constants;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.responsecode.ResponseCode;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class PostAnalyticsController {

    private final PostAnalyticsServiceImpl postAnalyticsService;
    public PostAnalyticsController(PostAnalyticsServiceImpl postAnalyticsService){
        this.postAnalyticsService = postAnalyticsService;
    }
    /**
     * @param rootOrg Name of root Organization
     * @param org Name of Organization inside rootOrg
     * @param postKind  Blog
     * @param postStatus Active,InActive
     * @param startDate Optional format = yyyy-MM-dd HH:mm:ss timeZone = GMT
     * @param endDate Optional format = yyyy-MM-dd HH:mm:ss timeZone = GMT
     * @param sources Optional format = id,postContent.title,postCreator.name
     * @return List of Post Analytic contents
     */
    @GetMapping("/v1/post/{postKind}/{postStatus}")
    public ResponseEntity<?> getPostAnalyticContents(@RequestHeader("rootOrg") String rootOrg,
                                                  @RequestHeader("org") String org,
                                                  @PathVariable("postKind") String postKind,
                                                  @PathVariable("postStatus") String postStatus,
                                                  @RequestParam(value = "startDate", required = false) Timestamp startDate,
                                                  @RequestParam(value = "endDate", required = false) Timestamp endDate,
                                                     @RequestParam(value = "sources",required = false , defaultValue = "id,postContent.title,postCreator.name") String[] sources) {
        HttpStatus status;
        Response resp = new Response();
        resp.setVer("v1");
        resp.setId("api.getPostAnalyticContents");
        resp.setTs(ProjectUtil.getFormattedDate());
        try {
            // Parsing in format required by ElasticSearch
            String sDate = LexProjectUtil.getEsFormattedDateForPostAnalyticContent(startDate);
            String eDate = LexProjectUtil.getEsFormattedDateForPostAnalyticContent(endDate);
            List<Map<String, Object>> result = postAnalyticsService.getPostAnalyticContent(rootOrg, org, postKind ,postStatus ,sDate,eDate,sources);
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
