package com.infosys.repository;

import com.infosys.model.cassandra.UserContentRatingModel;
import com.infosys.model.cassandra.UserContentRatingPrimaryKeyModel;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface UserContentRatingRepository extends CassandraRepository<UserContentRatingModel, UserContentRatingPrimaryKeyModel> {

    @Query("Select avg(rating) as \"averageRating\",count(rating) as \"viewCount\" from user_content_rating where root_org = ?0 and content_id = ?1")
    public Map<String,Object> getAvgRatingAndRatingCountForContentId(String rootOrg, String contentId );

    @Query("Select content_id, avg(rating) as \"averageRating\",count(rating) as \"viewCount\" from user_content_rating where root_org = ?0 and content_id in ?1 group by content_id")
    public List<Map<String,Object>> getAvgRatingAndRatingCountForContentIds(String rootOrg, List<String> contentId );

}
