package com.infosys.model.cassandra;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.io.Serializable;
import java.util.Date;

@Table("user_content_rating")
public class UserContentRatingModel implements Serializable {

    public UserContentRatingPrimaryKeyModel getPrimaryKey() {
        return primaryKey;
    }


    public void setPrimaryKey(UserContentRatingPrimaryKeyModel primaryKey) {
        this.primaryKey = primaryKey;
    }


    public Date getLastUpdatedOn() {
        return lastUpdatedOn;
    }


    public void setLastUpdatedOn(Date lastUpdatedOn) {
        this.lastUpdatedOn = lastUpdatedOn;
    }


    /**
     *
     */
    private static final long serialVersionUID = 6029654870295464031L;

    @PrimaryKey
    private UserContentRatingPrimaryKeyModel  primaryKey;


    @Column("rating")
    private Float rating;

    @Column("last_updated_on")
    private Date lastUpdatedOn;





    public Float getRating() {
        return rating;
    }


    public void setRating(Float rating) {
        this.rating = rating;
    }


    public UserContentRatingModel(UserContentRatingPrimaryKeyModel primaryKey, Float rating) {
        this.primaryKey = primaryKey;
        this.rating = rating;
        this.lastUpdatedOn = new Date();
    }





}
