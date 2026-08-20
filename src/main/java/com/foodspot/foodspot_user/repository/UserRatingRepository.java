package com.foodspot.foodspot_user.repository;

import com.foodspot.foodspot_user.model.UserRatingModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List; 
import java.util.Optional;

@Repository
public interface UserRatingRepository extends MongoRepository<UserRatingModel, String> {
    
    // Checks if a specific user has already reviewed a specific spot
    Optional<UserRatingModel> findBySpotIdAndUserId(String spotId, String userId);

    // Fetch all reviews for a specific spot (Needed for counting users and calculating averages)
    List<UserRatingModel> findBySpotId(String spotId);

    // Fetch reviews for multiple spot IDs all at once (Optimized for home page batch loading)
    List<UserRatingModel> findBySpotIdIn(List<String> spotIds);

    // Fetch all reviews submitted by a specific user across a list of spots
    List<UserRatingModel> findByUserIdAndSpotIdIn(String userId, List<String> spotIds);

    // Count how many spots a specific user has rated
    long countByUserId(String userId);
}