/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.model.cassandra;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.Date;


@Table("user_discrete_points")
public class DiscretePointsModel {

    @PrimaryKey
    private DiscretePointsPrimaryKeyModel primaryKey;

    @Column("points")
    private Integer points;

    @Column("given_on")
    private Date givenOn;

    @Column("points_for")
    private String points_for;

    public DiscretePointsModel(DiscretePointsPrimaryKeyModel primaryKey, Integer points, Date givenOn,
                               String points_for) {
        super();
        this.primaryKey = primaryKey;
        this.points = points;
        this.givenOn = givenOn;
        this.points_for = points_for;
    }

    public DiscretePointsModel() {
        super();
    }

    public DiscretePointsPrimaryKeyModel getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(DiscretePointsPrimaryKeyModel primaryKey) {
        this.primaryKey = primaryKey;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public Date getGivenOn() {
        return givenOn;
    }

    public void setGivenOn(Date givenOn) {
        this.givenOn = givenOn;
    }

    public String getPoints_for() {
        return points_for;
    }

    public void setPoints_for(String points_for) {
        this.points_for = points_for;
    }

}
