package com.foodspot.foodspot_user.repository;

import com.foodspot.foodspot_user.model.NewSpotModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NewSpotRepository extends MongoRepository<NewSpotModel, String> {
    
    // Spring Data automatically implements this count query for the new_spots collection
    long countByUserId(String userId);
}