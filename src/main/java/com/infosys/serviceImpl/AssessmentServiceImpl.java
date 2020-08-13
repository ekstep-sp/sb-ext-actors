/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import com.infosys.exception.ApplicationLogicError;
import com.infosys.exception.BadRequestException;
import com.infosys.repository.AssessmentRepository;
import com.infosys.repository.BadgeRepository;
import com.infosys.service.AssessmentService;
import com.infosys.service.UserUtilityService;
import com.infosys.util.LexJsonKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.PropertiesCache;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class AssessmentServiceImpl implements AssessmentService {

	@Autowired
	AssessmentRepository repository;
	
	@Autowired
	BadgeRepository bRepository;

    @Autowired
    UserUtilityService userUtilService;

	SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
	private PropertiesCache properties = PropertiesCache.getInstance();
	private String bodhiKeyspace = LexJsonKey.BODHI_DB_KEYSPACE;
	private String assessmentMaster = properties.getProperty(LexJsonKey.Assessment_Master);
	private String quizMaster = properties.getProperty(LexJsonKey.Quiz_Master);

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> submitAssessment(Map<String, Object> data) {
		Map<String, Object> ret = new HashMap<String, Object>();
		try {
			Integer correct = 0;
			Integer blank = 0;
			Integer inCorrect = 0;
			Double result = 0d;

			// assessment meta
            Map<String, Object> source = userUtilService.getMetaByIDandSource(data.get("identifier").toString(),
					new String[] { "artifactUrl", "collections", "contentType" });

			// assessment answers
			Map<String, Object> answers = new HashMap<String, Object>();
			if ((boolean) data.get("isAssessment"))
				answers = repository
						.getAssessmentAnswerKey(source.get("artifactUrl").toString().replaceAll(".json", "-key.json"));
			else
				answers = repository.getQuizAnswerKey(data);

			for (Map<String, Object> question : (List<Map<String, Object>>) data.get("questions")) {
				List<String> marked = new ArrayList<String>();
				if (question.containsKey("questionType")) {
					if (question.get("questionType").toString().toLowerCase().equals("mtf")) {
						for (Map<String, Object> options : (List<Map<String, Object>>) question.get("options")) {
							if(options.containsKey("response") && !options.get("response").toString().isEmpty())
								marked.add(options.get("optionId").toString() + "-"
										+ options.get("text").toString().toLowerCase() + "-" + options.get("response").toString().toLowerCase());
						}
					} else if (question.get("questionType").toString().toLowerCase().equals("fitb")) {
						for (Map<String, Object> options : (List<Map<String, Object>>) question.get("options")) {
							if(options.containsKey("response") && !options.get("response").toString().isEmpty())
								marked.add(options.get("optionId").toString() + "-" + options.get("response").toString().toLowerCase());
						}
					}
					else if (question.get("questionType").toString().toLowerCase().equals("mcq-sca") || question.get("questionType").toString().toLowerCase().equals("mcq-mca")){
						for (Map<String, Object> options : (List<Map<String, Object>>) question.get("options")) {
							if ((boolean) options.get("userSelected"))
								marked.add(options.get("optionId").toString());
						}
					}
				} else {
					for (Map<String, Object> options : (List<Map<String, Object>>) question.get("options")) {
						if ((boolean) options.get("userSelected"))
							marked.add(options.get("optionId").toString());
					}
				}
				
				if (marked.size() == 0)
					blank++;
				else {
					List<String> answer = (List<String>) answers.get(question.get("questionId"));
					if (answer.size() > 1)
						Collections.sort(answer);
					if (marked.size() > 1)
						Collections.sort(marked);
					if (answer.equals(marked))
						correct++;
					else
						inCorrect++;
				}
			}
			result = ((correct * 100d) / (correct + blank + inCorrect));

//			result=100d;
			Map<String, Object> persist = new HashMap<String, Object>();
			persist.put("parent",
					((List<Object>) source.get("collections")).size() > 0
							? ((List<Map<String, Object>>) (source.get("collections"))).get(0).get("identifier")
							: "");
			
			persist.put("result", result);
			persist.put("sourceId", data.get("identifier"));
			persist.put("title", data.get("title"));
			persist.put("email", data.get("userEmail"));
			persist.put("correct", correct);
			persist.put("blank", blank);
			persist.put("incorrect", inCorrect);

			if ((boolean) data.get("isAssessment")) {
				Map<String, Object> tempSource = null;
				// get parent data for assessment
				if (((List<Object>) source.get("collections")).size() > 0) {
                    tempSource = userUtilService
							.getMetaByIDandSource(((List<Map<String, Object>>) (source.get("collections"))).get(0)
									.get("identifier").toString(), new String[] { "contentType", "collections" });
					persist.put("parentContentType", tempSource.get("contentType"));

				} else {
					persist.put("parentContentType", "");
				}
				// insert into assessment table
				repository.insertQuizOrAssessment(bodhiKeyspace, assessmentMaster, persist, true);

				if (tempSource != null && result >= 60) {
					if (tempSource.get("contentType").equals("Course")) {
						// insert certificate and medals
						String courseId = ((List<Map<String, Object>>) (source.get("collections"))).get(0)
								.get("identifier").toString();
						boolean parent = ((List<Object>) tempSource.get("collections")).size() > 0 ? true : false;
                        List<String> programId = new ArrayList<String>();
                        for (Map<String, Object> collection : ((List<Map<String, Object>>) (tempSource
                                .get("collections")))) {
                            programId.add(collection.get("identifier").toString());
                        }
						bRepository.insertInBadges(courseId, programId, data.get("userEmail").toString(), parent);
						bRepository.insertCourseAndQuizBadge(data.get("userEmail").toString(), "Course",
								data.get("identifier").toString());
					}
				}
			} else {
				// insert into quiz table
				persist.remove("parent");
				repository.insertQuizOrAssessment(bodhiKeyspace, quizMaster, persist, false);
				bRepository.insertCourseAndQuizBadge(data.get("userEmail").toString(), "Quiz",
						data.get("identifier").toString());
			}

			ret.put("result", result);
			ret.put("correct", correct);
			ret.put("inCorrect", inCorrect);
			ret.put("blank", blank);
			ret.put("total", blank + inCorrect + correct);
			ret.put("passPercent", 60);

		} catch (Exception e) {
			ret.put("error", e.getMessage());
			e.printStackTrace();
			ProjectLogger.log("Error : " + e.getMessage(), e);
		}
		return ret;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> submitAssessmentByIframe(Map<String, Object> request) throws Exception {
		Map<String, Object> response = new HashMap<>();
		boolean invalidRequest = true;
		boolean resultWrong = true;
		String errorMsg = "";
		Double result = (Double) request.get("result");

		// Validating RequestBody
		if (request.get("parent") == null) {
			errorMsg = "COURSE_ID_CANT_BE_NULL";
		} else if (request.get("email") == null) {
			errorMsg = "EMAIL_ID_CANT_BE_NULL";
		} else if (request.get("sourceId") == null) {
			errorMsg = "ASSESSMENT_ID_CANT_BE_NULL";
		} else if (request.get("title") == null) {
			errorMsg = "ASSESSMENT_NAME_CANT_BE_NULL";
		} else if (request.get("correct") == null) {
			errorMsg = "CORRECT_COUNT_CANT_BE NULL";
		} else if (request.get("incorrect") == null) {
			errorMsg = "INCORRECT_COUNT_CANT_BE NULL";
		} else if (request.get("blank") == null) {
			errorMsg = "NOT_ANSWERED_COUNT_CANT_BE NULL";
		} else if (request.get("result") == null) {
			invalidRequest = true;
			errorMsg = "RESULT_PERCENTAGE_CANT_BE NULL";
		} else if (request.get("parentContentType") == null) {
			errorMsg = "CONTENT_TYPE_CANT_BE_NULL";
		} else if (!(request.get("parentContentType").toString().equals("Course")
				|| request.get("parentContentType").toString().equals("Learning Module"))) {
			errorMsg = "CONTENT_TYPE_CAN_ONLY_BE_COURSE_OR_LEARNING_MODULE";
		} else if ((Integer) request.get("correct") < 0) {
			errorMsg = "CORRECT_COUNT_MUST_BE_POSITIVE";
		} else if ((Integer) request.get("incorrect") < 0) {
			errorMsg = "INCORRECT_COUNT_MUST_BE_POSITIVE";
		} else if ((Integer) request.get("blank") < 0) {
			errorMsg = "NOT_ANSWERED_COUNT_MUST_BE_POSITIVE";
		} else if (resultWrong) {
			Integer correctCount = (Integer) request.get("correct");
			Integer incorrectCount = (Integer) request.get("incorrect");
			Integer notAnsweredCount = (Integer) request.get("blank");
			Double resultValidation = 0d;
			resultValidation = (100d * correctCount) / (correctCount + incorrectCount + notAnsweredCount);
			resultValidation = BigDecimal.valueOf(resultValidation).setScale(2, BigDecimal.ROUND_HALF_EVEN)
					.doubleValue();
			if (result.compareTo(resultValidation) != 0) {
				errorMsg = "RESULT_IS_WRONG";
			} else {
				resultWrong = false;
				invalidRequest = false;
			}
		} else {
			invalidRequest = false;
		}

		if (invalidRequest) {
			throw new BadRequestException(errorMsg);
		}

		try {
            Map<String, Object> source = userUtilService.getMetaByIDandSource(request.get("sourceId").toString(),
					new String[] { "artifactUrl", "collections", "contentType" });
            Map<String, Object> tempSource = userUtilService.getMetaByIDandSource(
					((List<Map<String, Object>>) (source.get("collections"))).get(0).get("identifier").toString(),
					new String[] { "contentType", "collections" });
			repository.insertQuizOrAssessment(bodhiKeyspace, assessmentMaster, request, true);
			if (tempSource != null && result >= 60) {
				if (tempSource.get("contentType").equals("Course")) {
					// insert certificate and medals
					String courseId = request.get("parent").toString();
					boolean parent = ((List<Object>) tempSource.get("collections")).size() > 0 ? true : false;
                    List<String> programId = new ArrayList<String>();
                    for (Map<String, Object> collection : ((List<Map<String, Object>>) (tempSource
                            .get("collections")))) {
                        programId.add(collection.get("identifier").toString());
                    }
					bRepository.insertInBadges(courseId, programId, request.get("email").toString(), parent);
				}
			}
			response.put("message", "Record Inserted Sucessfully !!");
		} catch (Exception e) {
			throw new ApplicationLogicError("REQUEST_COULD_NOT_BE_PROCESSED");
		}

		return response;
	}

	// A method to Format Data in the FrontEndFormat
	private List<Map<String, Object>> getAssessments(List<Map<String, Object>> result) {
		List<Map<String, Object>> assessments = new ArrayList<>();
		for (Map<String, Object> map : result) {
			Map<String, Object> assessmentData = new HashMap<>();
			String res = map.get("result_percent").toString();
			assessmentData.put("result", new BigDecimal(res).setScale(2, BigDecimal.ROUND_UP));
			assessmentData.put("correctlyAnswered", map.get("correct_count"));
			assessmentData.put("wronglyAnswered", map.get("incorrect_count"));
			assessmentData.put("notAttempted", map.get("not_answered_count"));
			assessmentData.put("takenOn", map.get("ts_created"));
			assessments.add(assessmentData);
		}
		return assessments;
	}

	/*
	 * A service to produce a JSON response with processed Data on all Assessments
	 * such as fristPassedts,maxScore,etc , and a list of Past Assessments.
	 */
	@Override
	public Map<String, Object> getAssessmentByContentUser(String course_id, String user_id)
			throws ApplicationLogicError {
		Map<String, Object> result = new TreeMap<>();
		try {
			// get all submission data from cassandra
			List<Map<String, Object>> assessmentResults = repository.getAssessmetbyContentUser(course_id, user_id);
			// retain only those fields that need to be sent to front end
			List<Map<String, Object>> assessments = getAssessments(assessmentResults);

			// initialize variables to calculate first attempt and max score
			Integer noOfAttemptsForPass = 0;
			Integer noOfAttemptsForMaxScore = 0;
			boolean passed = false;
			Object firstPassTs = null;
			BigDecimal max = new BigDecimal(-Double.MIN_VALUE);
			Object maxScoreTs = null;

			/*
			 * Logic to Find The First Time Passed and The Max Score Attained along with
			 * Their No of Attempts and Timestamps
			 */
			for (int i = assessments.size() - 1; i > -1; i--) {
				Map<String, Object> row = assessments.get(i);
				BigDecimal percentage = (BigDecimal) row.get("result");
				/*
				 * Logic to Obtain the First Pass using a Passed flag to attain the Attempts as
				 * well as the first Time passed Time Stamp
				 */
				if (!passed) {
					noOfAttemptsForPass++;
					if (percentage.doubleValue() >= 60.0) {
						passed = true;
						firstPassTs = row.get("takenOn");
					}
				}

				/*
				 * Logic to Obtain the max scored assessment comparison to attain the Attempts
				 * as well as the Max Scored Assessment Time Stamp
				 */
				if (max.compareTo(percentage) < 0) {
					max = (BigDecimal) row.get("result");
					maxScoreTs = row.get("takenOn");
					noOfAttemptsForMaxScore = (assessments.size() - i);
				}
			}

			/* Populating the Response to give Processed Data to Front End */
			if (assessments.size() > 0) {
				if (passed) {
					result.put("firstPassOn", firstPassTs);
					result.put("attemptsToPass", noOfAttemptsForPass);
				}

				result.put("maxScore", max);
				result.put("maxScoreAttainedOn", maxScoreTs);
				result.put("attemptsForMaxScore", noOfAttemptsForMaxScore);
			}
			result.put("pastAssessments", assessments);
		} catch (NullPointerException e) {
			throw new ApplicationLogicError("REQUEST COULD NOT BE PROCESSED");
		}
		return result;
	}

	/*
	 * @Override public List<Map<String, Object>> getAssessmentByContentUser(String
	 * course_id, String user_id) throws Exception { List<Map<String,Object>> lst =
	 * new ArrayList<Map<String,Object>>(); ProjectLogger.
	 * log("Fetching Data from assessment_user_master view started at "
	 * +LocalDateTime.now().toString(),LoggerEnum.INFO); Map<String,Object>
	 * requestMap = new HashMap<>(); try {
	 * 
	 * requestMap.put("parent_source_id",course_id);
	 * requestMap.put("user_id",user_id); Response response =
	 * cassandraOperation.getRecordsByProperties(bodhiKeyspace,
	 * "assessment_by_content_user",requestMap);
	 * 
	 * @SuppressWarnings("unchecked") List<Map<String,Object>> rslst =
	 * (List<Map<String, Object>>) response.getResult().get("response");
	 * for(Map<String,Object> map: rslst) { Map<String,Object> rowmap = new
	 * HashMap<>(); rowmap.put("Assessment Name",map.get("source_title"));
	 * rowmap.put("Result",map.get("result_percent"));
	 * rowmap.put("Passing Marks",map.get("pass_percent"));
	 * rowmap.put("Correctly Answered",map.get("correct_count"));
	 * rowmap.put("Wrongly Answered",map.get("incorrect_count"));
	 * rowmap.put("Not Attempted",map.get("not_answered_count"));
	 * rowmap.put("Taken On",map.get("ts_created").toString()); lst.add(rowmap); }
	 * }catch (Exception e) { throw new
	 * Exception("PATH_WRONG: parent_course_id or user_id invalid"); }
	 * ProjectLogger.log("Fetching Data from assessment_user_master view ended at "
	 * +LocalDateTime.now().toString(),LoggerEnum.INFO); System.out.println(lst);
	 * return lst; }
	 */
}
