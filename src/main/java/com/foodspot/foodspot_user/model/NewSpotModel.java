package com.foodspot.foodspot_user.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "new_spots")
public class NewSpotModel {

    @Id
    private String id;
    
    private String userId; // Links the spot to the user who added it
    private String name;
    private String place;
    private String district;
    private String state;
    private List<String> categories; // Handles multiple checkbox selections
    private String phone;
    private String speciality;
    private String mapLink;
    private String directions;
    
    private String coverPhotoUrl;   // For single cover photo
    private List<String> photoUrls; // For additional food photos
    
    private LocalDateTime createdAt = LocalDateTime.now();

    public NewSpotModel() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPlace() { return place; }
    public void setPlace(String place) { this.place = place; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getSpeciality() { return speciality; }
    public void setSpeciality(String speciality) { this.speciality = speciality; }

    public String getMapLink() { return mapLink; }
    public void setMapLink(String mapLink) { this.mapLink = mapLink; }

    public String getDirections() { return directions; }
    public void setDirections(String directions) { this.directions = directions; }

    public String getCoverPhotoUrl() { return coverPhotoUrl; }
    public void setCoverPhotoUrl(String coverPhotoUrl) { this.coverPhotoUrl = coverPhotoUrl; }

    public List<String> getPhotoUrls() { return photoUrls; }
    public void setPhotoUrls(List<String> photoUrls) { this.photoUrls = photoUrls; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}