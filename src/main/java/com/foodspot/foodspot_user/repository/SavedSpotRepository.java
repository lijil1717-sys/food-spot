package com.foodspot.foodspot_user.repository;

import com.foodspot.foodspot_user.model.SavedSpotModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedSpotRepository extends MongoRepository<SavedSpotModel, String> {
    List<SavedSpotModel> findByUserId(String userId);
    Optional<SavedSpotModel> findByUserIdAndSpotId(String userId, String spotId);
    void deleteByUserIdAndSpotId(String userId, String spotId);

    // ---> ADD THIS METHOD <---
    long countByUserId(String userId);
}