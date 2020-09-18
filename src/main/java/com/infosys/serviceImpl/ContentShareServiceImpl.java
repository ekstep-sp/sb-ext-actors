package com.infosys.serviceImpl;

import com.infosys.cassandra.CassandraOperation;
import com.infosys.exception.ApplicationLogicError;
import com.infosys.exception.BadRequestException;
import com.infosys.exception.NoContentException;
import com.infosys.exception.PropertiesNotFoundException;
import com.infosys.helper.ServiceFactory;
import com.infosys.service.ContentShareService;
import com.infosys.util.LexJsonKey;
import com.infosys.util.Util;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.sunbird.common.Constants;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.responsecode.ResponseCode;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;

@Service
public class ContentShareServiceImpl implements ContentShareService {

    private static CassandraOperation cassandraOperation = ServiceFactory.getInstance();
    private Util.DbInfo contentShareUrl = Util.dbInfoMap.get(LexJsonKey.PUBLIC_CONTENT_DATA);
    // Request Parameters List
    private List<String> parmas = Arrays.asList("rootOrg","org","wid","lexId","pageType","contentType");

    // Content Public Url Marker
    @Value("${public_content_url}")
    private String publicUrl;

    // Root Org to Domain Mapping
    @Value("${domain_mapping}")
    private String domainDetails;

    private Map<String, String> domainMappping = new HashMap<>();

    @PostConstruct
    private void init() {
        try {
            ProjectLogger.log("RootOrg and Domain Mapping : " + domainDetails, LoggerEnum.INFO);
            // Initialize RootOrg and Domain Name as Map
            if(domainDetails != null && domainDetails.length() > 0) {
                String[] domains = domainDetails.split(",");
                if (domains.length > 0) {
                    for (String domain : domains) {
                        String[] domainData = domain.split(";");
                        domainMappping.put(domainData[0], domainData[1]);
                    }
                }
            } else {
                ProjectLogger.log("Domain Mapping Unavailable. Content Share Apis may not work.", LoggerEnum.WARN);
            }
        } catch (Exception e) {
            ProjectLogger.log("Domain Mapping is not initialized correctly. Content Share Apis may not work." ,e);
        }
    }

    // Service to create content public Url and Shareable Token
    @Override
    public Map<String, Object> createContentPublicUrl(Map<String, Object> request) throws Exception {

        Map<String, Object> responseMap = new HashMap<>();

        // validate all the request parameters
        validateRequestParamsForCreateblicUrl(request);

        // extract all the required parameters
        String pageType = (String) request.get("pageType");
        String contentType = (String) request.get("contentType");
        String lexId = (String) request.get("lexId");
        String rootOrg = (String) request.get("rootOrg");
        String org = (String) request.get("org");
        String wid = (String) request.get("wid");

        // validate the property Public Url
        String domain = getDomainForRootOrg(rootOrg);
        if(publicUrl == null || publicUrl.isEmpty()) {
            throw new PropertiesNotFoundException(MessageFormat.format("Property {0} not found.", LexJsonKey.PUBLIC_CONTENT_URL));
        }

        // create a Primary Key for the Token
        // It is an essential step as for the same data, same UUID must be generated
        String primaryKeyParams = pageType + contentType + lexId + rootOrg + org + wid;

        // Generate UUID on basis of above Primary Key String
        String shareableToken = UUID.nameUUIDFromBytes(primaryKeyParams.getBytes(StandardCharsets.UTF_8)).toString();

        // Generating Shareable Url
        String shareableUrl = domain + "/" + publicUrl + "/" + pageType + "/" + shareableToken;

        // Check if the UUID is already present in the database for the Primary Key String
        Response response = cassandraOperation.getRecordById(contentShareUrl.getKeySpace(),
                contentShareUrl.getTableName(), shareableToken);
        // If not found then insert the badge
        List<Map<String, Object>> sharedContentDetails = (List<Map<String, Object>>) response.get(Constants.RESPONSE);

        // check and create the Shareable Token and Url id data not present
        if (sharedContentDetails.size() == 0 || sharedContentDetails.get(0) == null || sharedContentDetails.get(0).isEmpty()) {

            ProjectLogger.log("Creating new Content Shareable URL.");
            Map<String, Object> data = new HashMap<>();
            data.put("id", shareableToken);
            data.put("wid", wid);
            data.put("lexid", lexId);
            data.put("root_org", rootOrg);
            data.put("org", org);
            data.put("page_type", pageType);
            data.put("content_type", contentType);
            data.put("created_on", Calendar.getInstance().getTime());
            data.put("last_updated_on", Calendar.getInstance().getTime());
            data.put("count", 0);
            data.put("shareable_url", shareableUrl);

            response = cassandraOperation.insertRecord(contentShareUrl.getKeySpace(), contentShareUrl.getTableName(), data);
        } else {
            ProjectLogger.log("Found Shareable Content with the details. Returning the existing Content Shareable URL.");
        }

        if (!response.getResponseCode().equals(ResponseCode.OK)) {
            throw new ApplicationLogicError("Failed to insert content share details.");
        } else {
            responseMap.put("shareableToken", shareableToken);
            responseMap.put("shareableUrl", shareableUrl);
        }
        return responseMap;
    }

    @Override
    public Map<String, Object> verifyContentPublicToken(String publicToken, String rootOrg, String org, String pageType) throws Exception {
        Map<String, Object> responseMap = new HashMap<>();

        // Validate if the Token is provided or not
        if(publicToken == null || publicToken.isEmpty()) {
            throw new BadRequestException("PLEASE PROVIDE Content Share ID.");
        }

        if(rootOrg == null || rootOrg.isEmpty()) {
            throw new BadRequestException("PLEASE PROVIDE rootOrg.");
        }

        if(org == null || org.isEmpty()) {
            throw new BadRequestException("PLEASE PROVIDE org.");
        }
        //        pageType = pageType.toLowerCase();
        //        if(!pageType.equals("v") && !pageType.equals("o")) {
        //            throw new BadRequestException("PLEASE PROVIDE pageType value as 'v' or 'o'.");
        //        }

        Map<String, Object> reqParams = new HashMap<>();
        reqParams.put("id",publicToken);
        reqParams.put("root_org", rootOrg);
        reqParams.put("org",org);
//        reqParams.put("page_type",pageType);

        // Get Record from Cassandra for the Shareable Token
        Response response = cassandraOperation.getRecordsByProperties(contentShareUrl.getKeySpace(),
                contentShareUrl.getTableName(), reqParams);


        List<Map<String, Object>> sharedContentDetails = (List<Map<String, Object>>) response.get(Constants.RESPONSE);
        if (sharedContentDetails.size() == 0 || sharedContentDetails.get(0) == null || sharedContentDetails.get(0).isEmpty()) {
            throw new NoContentException("No Content Found with the provided Token.");
        } else {
            ProjectLogger.log("Found Shareable Content with the details. Returning Content Data.");
            // IF content is found with the Shareable Token, update the token access time and count.
            Map<String, Object> contentDatails = sharedContentDetails.get(0);
            Map<String, Object> updatedData = new HashMap<>();
            updatedData.put("id", contentDatails.get("id"));
            updatedData.put("count", (int)contentDatails.get("count")+1);
            updatedData.put("last_updated_on", Calendar.getInstance().getTime());

            ProjectLogger.log("Updating count and timestamp for the Shareable Content Found.");
            cassandraOperation.updateRecord(contentShareUrl.getKeySpace(),
                    contentShareUrl.getTableName(),updatedData);

            // Add date to the response
            responseMap.put("lexId", contentDatails.get("lexid"));
            responseMap.put("shareableUrl", contentDatails.get("shareable_url"));
        }

        return responseMap;
    }

    // Validate all the parameters of the request for Public Url Creation
    private void validateRequestParamsForCreateblicUrl(Map<String, Object> reqParams) {

        for (String param : parmas) {
            String value = (String) reqParams.get(param);
            if(value == null || value.isEmpty()) {
                throw new BadRequestException(MessageFormat.format("PLEASE PROVIDE {0}.", param));
            }
        }

        String pageType = ((String) reqParams.get("pageType")).toLowerCase();
        if (!pageType.equals("v") && !pageType.equals("o")) {
            throw new BadRequestException("PLEASE PROVIDE pageType correct value.");
        } else {
            reqParams.put("pageType", pageType);
        }
    }


    // Get Domain for the sprecified Root Org
    private String getDomainForRootOrg(String rootOrg) {

        String domain = "";
        if (domainMappping.size() > 0) {
            domain = domainMappping.get(rootOrg);
        } else {
            throw new PropertiesNotFoundException(MessageFormat.format("Property {0} not found.", LexJsonKey.DOMAIN_MAPPING));
        }

        if(domain == null || domain.isEmpty()) {
            throw new PropertiesNotFoundException("Domain not configured for this Root Org.");
        }
        return domain;
    }
}
