package com.foodspot.foodspot_user.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "SavedSpots")
public class SavedSpotModel {

    @Id
    private String id;
    private String userId;
    private String spotId;

    public SavedSpotModel() {}

    public SavedSpotModel(String userId, String spotId) {
        this.userId = userId;
        this.spotId = spotId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getSpotId() { return spotId; }
    public void setSpotId(String spotId) { this.spotId = spotId; }
}