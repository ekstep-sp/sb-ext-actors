/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.model;

import java.util.HashSet;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CourseRecommendation implements Comparable<CourseRecommendation> {
    private double score;
    private Map<String, Object> course;
	private HashSet<String> reasonsForRecommendation;

    public CourseRecommendation() {

    }

    public CourseRecommendation(double score, Map<String, Object> course, HashSet<String> reasonsForRecommendation) {
        super();
        this.score = score;
        this.course = course;
        this.reasonsForRecommendation = reasonsForRecommendation;
    }

    public double getScore() {
        return score;
    }
	public void setScore(double score) {
		this.score = score;
	}
	public Map<String, Object> getCourse() {
		return course;
	}
	public void setCourse(Map<String, Object> course) {
		this.course = course;
	}
	public HashSet<String> getReasonsForRecommendation() {
		return reasonsForRecommendation;
	}
	public void setReasonsForRecommendation(HashSet<String> reasonsForRecommendation) {
		this.reasonsForRecommendation = reasonsForRecommendation;
	}

    @Override
    public int compareTo(CourseRecommendation arg0) {
        if ((this.score - arg0.score) > 0) {
            return -1;
        } else {
            return 1;
        }
    }
}
