/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
//package com.infosys.model.cassandra;
//
//import org.springframework.data.cassandra.mapping.Column;
//import org.springframework.data.cassandra.mapping.PrimaryKey;
//import org.springframework.data.cassandra.mapping.Table;
//
//@Table("total_points")
//public class TotalPoints {
//	@PrimaryKey("email_id")
//	private String emailId;
//	
//	@Column("collaborative_points")
//	private Long collaborativePoints;
//	@Column("learning_points")
//	private Long learningPoints;
//
//	
//	public String getEmailId() {
//		return emailId;
//	}
//
//
//	public void setEmailId(String emailId) {
//		this.emailId = emailId;
//	}
//
//
//	public Long getCollaborativePoints() {
//		return collaborativePoints;
//	}
//
//
//	public void setCollaborativePoints(Long collaborativePoints) {
//		this.collaborativePoints = collaborativePoints;
//	}
//
//
//	public Long getLearningPoints() {
//		return learningPoints;
//	}
//
//
//	public void setLearningPoints(Long learningPoints) {
//		this.learningPoints = learningPoints;
//	}
//
//
//	public TotalPoints(String emailId, Long collaborativePoints, Long learningPoints) {
//		super();
//		this.emailId = emailId;
//		this.collaborativePoints = collaborativePoints;
//		this.learningPoints = learningPoints;
//	}
//
//
//	public TotalPoints() {
//	}
//
//}
