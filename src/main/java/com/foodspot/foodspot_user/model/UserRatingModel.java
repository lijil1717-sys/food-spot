package com.foodspot.foodspot_user.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "user_ratings") // Match your exact MongoDB collection name here
public class UserRatingModel {

    @Id
    private String id;

    @Field("spotId")
    private String spotId;

    @Field("userId")
    private String userId;

    @Field("ratings")
    private Integer ratings;

    @Field("review")
    private String review;

    // --- GETTERS AND SETTERS ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSpotId() { return spotId; }
    public void setSpotId(String spotId) { this.spotId = spotId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Integer getRatings() { return ratings; }
    public void setRatings(Integer ratings) { this.ratings = ratings; }

    public String getReview() { return review; }
    public void setReview(String review) { this.review = review; }
}