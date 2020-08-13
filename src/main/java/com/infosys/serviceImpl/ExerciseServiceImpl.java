/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import com.datastax.driver.core.utils.UUIDs;
import com.infosys.exception.InvalidDataInputException;
import com.infosys.exception.ResourceNotFoundException;
import com.infosys.model.cassandra.UserExerciseLastModel;
import com.infosys.model.cassandra.UserExerciseLastPrimaryKeyModel;
import com.infosys.repository.ExerciseBatchRepository;
import com.infosys.repository.ExerciseRepository;
import com.infosys.repository.UserExerciseRepository;
import com.infosys.repository.UserRepository;
import com.infosys.service.ExerciseService;
import com.infosys.service.UserUtilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.sunbird.common.models.util.ProjectLogger;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class ExerciseServiceImpl implements ExerciseService {

	@Autowired
	ExerciseRepository exerciseRepo;

	@Autowired
	UserExerciseRepository userExerrciseRepo;

	@Autowired
	UserRepository userRepo;

	@Autowired
	ExerciseBatchRepository<UserExerciseLastModel> exerciseBatchRepo;
	
	@Autowired
	UserUtilityService userUtilService;

	SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS");

	private String validateData(Map<String, Object> data) throws Exception {
		String ret = "valid";
		int keys = 0;
		try {
			for (String key : data.keySet()) {
				switch (key) {
				case "result_percent":
					keys += 1;
					break;
				case "total_testcases":
					keys += 1;
					break;
				case "testcases_passed":
					keys += 1;
					break;
				case "testcases_failed":
					keys += 1;
					break;
				case "submission_type":
					keys += 1;
					break;
                    case "url":
                        keys += 1;
                        break;
				default:
					break;
				}
			}
            if (keys < 6)
				throw new InvalidDataInputException("fields missing");
		} catch (InvalidDataInputException e) {
			throw e;
		} catch (Exception e) {
			throw new Exception(e.getMessage());
		}
		return ret;
	}

	@Override
    public String insertSubmission(Map<String, Object> meta, String contentId, String userId)
			throws Exception {
		String ret = "Success";
		try {
			exerciseRepo.validateContentID(contentId);
			String userUUID = "";
			if (meta.get("user_id_type").toString().toLowerCase().equals("email")) {
				Map<String, Object> temp = userRepo.getUUIDFromEmail(userId);
				userUUID = temp.get("id").toString();
			} else {
				userUUID = userId;
				exerciseRepo.validateUserID(userUUID);
			}
			if (this.validateData(meta).toLowerCase().equals("valid")) {
                String url = meta.get("url").toString();
				UserExerciseLastPrimaryKeyModel pk = new UserExerciseLastPrimaryKeyModel(userUUID, contentId);
				UserExerciseLastModel exercise = new UserExerciseLastModel(pk, UUIDs.timeBased(), new Date(),
						Float.parseFloat(meta.get("result_percent").toString()), url,
						meta.get("submission_type").toString(),
						Integer.parseInt(meta.get("testcases_failed").toString()),
						Integer.parseInt(meta.get("testcases_passed").toString()),
						Integer.parseInt(meta.get("total_testcases").toString()), null, null, null, null, null);
				exerciseBatchRepo.insert(exercise);
			}
		} catch (InvalidDataInputException e) {
			throw e;
		} catch (Exception e) {
			ProjectLogger.log("Error : " + e.getMessage(), e);
			throw new Exception(e.getMessage());
		}
		return ret;
	}

	@Override
	public List<Map<String, Object>> getAllData(String userId, String contentId, String userIdType) throws Exception {
		List<Map<String, Object>> ret = new ArrayList<>();
		try {
			exerciseRepo.validateContentID(contentId);
			String userUUID = "";
			if (userIdType.toLowerCase().equals("email")) {
				Map<String, Object> temp = userRepo.getUUIDFromEmail(userId);
				userUUID = temp.get("id").toString();
			} else {
				userUUID = userId;
				exerciseRepo.validateUserID(userUUID);
			}
			ret = exerciseRepo.getAll(userUUID, contentId);
		} catch (InvalidDataInputException e) {
			throw e;
		} catch (Exception e) {
			ProjectLogger.log("Error : " + e.getMessage(), e);
			throw new Exception(e.getMessage());
		}
		return ret;
	}

	@Override
	public List<Map<String, Object>> getLatestData(String userId, String contentId, String userIdType)
			throws Exception {
		List<Map<String, Object>> ret = new ArrayList<>();
		try {
			exerciseRepo.validateContentID(contentId);
			String userUUID = "";
			if (userIdType.toLowerCase().equals("email")) {
				Map<String, Object> temp = userRepo.getUUIDFromEmail(userId);
				userUUID = temp.get("id").toString();
			} else {
				userUUID = userId;
				exerciseRepo.validateUserID(userUUID);
			}
			ret = exerciseRepo.getLatest(userUUID, contentId);
		} catch (InvalidDataInputException e) {
			throw e;
		} catch (Exception e) {
			ProjectLogger.log("Error : " + e.getMessage(), e);
			throw new Exception(e.getMessage());
		}
		return ret;
	}

	@Override
	public List<Map<String, Object>> getOneData(String userId, String contentId, String submissionId, String userIdType)
			throws Exception {
		List<Map<String, Object>> ret = new ArrayList<>();
		try {
			exerciseRepo.validateContentID(contentId);
			exerciseRepo.validateSubmissionID(submissionId);
			String userUUID = "";

			if (userIdType.toLowerCase().equals("email")) {
				Map<String, Object> temp = userRepo.getUUIDFromEmail(userId);
				userUUID = temp.get("id").toString();
			} else {
				userUUID = userId;
				exerciseRepo.validateUserID(userUUID);
			}
			ret = exerciseRepo.getOne(userUUID, contentId, submissionId);
			if (ret.size() == 0)
				throw new ResourceNotFoundException("No such data exists!");
		} catch (ResourceNotFoundException e) {
			throw e;
		} catch (InvalidDataInputException e) {
			throw e;
		} catch (Exception e) {
			ProjectLogger.log("Error : " + e.getMessage(), e);
			throw new Exception(e.getMessage());
		}
		return ret;
	}

	@Override
    public String insertFeedback(Map<String, Object> meta, String contentId, String userId,
                                 String submissionId) throws Exception {
		String ret = "Success";
		try {
			exerciseRepo.validateContentID(contentId);
			exerciseRepo.validateSubmissionID(submissionId);
			String userUUID = "";
			if (meta.get("user_id_type").toString().toLowerCase().equals("email")) {
				Map<String, Object> temp = userRepo.getUUIDFromEmail(userId);
				userUUID = temp.get("id").toString();
			} else {
				userUUID = userId;
				exerciseRepo.validateUserID(userUUID);
			}
            if (this.validateFeedback(meta).toLowerCase().equals("valid")) {
                String educatorUUID = "";
                if (meta.get("user_id_type").toString().toLowerCase().equals("email")) {
                    Map<String, Object> temp = userRepo.getUUIDFromEmail(meta.get("educator_id").toString());
                    educatorUUID = temp.get("id").toString();
                } else {
                    educatorUUID = meta.get("educator_id").toString();
                    exerciseRepo.validateUserID(educatorUUID);
                }
                String url = meta.get("url").toString();
				UUID uid = UUID.fromString(submissionId);
				Float result = (Float.parseFloat(meta.get("rating").toString()) * 100)
						/ Float.parseFloat(meta.get("max_rating").toString());

				UserExerciseLastPrimaryKeyModel pk = new UserExerciseLastPrimaryKeyModel(userUUID, contentId);
				UserExerciseLastModel exercise = new UserExerciseLastModel(pk, result, educatorUUID, new Date(),
						url, meta.get("feedback_type").toString(), uid);

				exerciseBatchRepo.insert(exercise);
			}
		} catch (InvalidDataInputException e) {
			throw e;
		} catch (Exception e) {
			ProjectLogger.log("Error : " + e.getMessage(), e);
			throw new Exception(e.getMessage());
		}
		return ret;
	}

	private String validateFeedback(Map<String, Object> data) throws Exception {
		String ret = "valid";
		int keys = 0;
		try {
			for (String key : data.keySet()) {
				switch (key) {
				case "educator_id":
					keys += 1;
					break;
				case "rating":
					keys += 1;
					break;
				case "max_rating":
					keys += 1;
					break;
				case "feedback_type":
					keys += 1;
					break;
                    case "url":
                        keys += 1;
                        break;
				default:
					break;
				}
			}
            if (keys < 5)
				throw new InvalidDataInputException("fields missing");
		} catch (InvalidDataInputException e) {
			throw e;
		} catch (Exception e) {
			throw new Exception(e.getMessage());
		}
		return ret;
	}

	@Override
	public List<Map<String, String>> getEducatorGroups(String educatorId, String userIdType) throws Exception {
		List<Map<String, String>> ret = new ArrayList<>();
		try {
			String educatorUUID = "";
			if (userIdType.toLowerCase().equals("email")) {
				Map<String, Object> temp = userRepo.getUUIDFromEmail(educatorId);
				educatorUUID = temp.get("id").toString();
			} else {
				educatorUUID = educatorId;
				exerciseRepo.validateUserID(educatorUUID);
			}
			ret = exerciseRepo.getGroups(educatorUUID);
		} catch (InvalidDataInputException e) {
			throw e;
		} catch (Exception e) {
			ProjectLogger.log("Error : " + e.getMessage(), e);
			throw e;
		}
		return ret;
	}

	@Override
    public Map<String, Object> getSubmissionsByGroups(String groupId, String contentId) throws Exception {
        Map<String, Object> ret = new HashMap<>();
		try {
			exerciseRepo.validateContentID(contentId);
			ret = exerciseRepo.getSubmissionsByGroups(groupId, contentId);
		} catch (InvalidDataInputException e) {
			throw e;
		} catch (Exception e) {
			ProjectLogger.log("Error : " + e.getMessage(), e);
			throw new Exception(e.getMessage());
		}
		return ret;
	}

    @Override
    public List<Map<String, Object>> getExerciseNotification(String userId, String userIdType) throws Exception {
        List<Map<String, Object>> ret = new ArrayList<>();
        String userUUID = "";
        if (userIdType.toLowerCase().equals("email")) {
            Map<String, Object> temp = userRepo.getUUIDFromEmail(userId);
            userUUID = temp.get("id").toString();
        } else {
            userUUID = userId;
            exerciseRepo.validateUserID(userUUID);
        }
        ret = exerciseRepo.getNotificationForUser(userUUID);
        return ret;
    }

}
