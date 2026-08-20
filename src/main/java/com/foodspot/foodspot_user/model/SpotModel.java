package com.foodspot.foodspot_user.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document(collection = "Spots")
public class SpotModel {

    @Id
    private String id;
    private String name;
    private String exactPlace;
    private String place;
    private String district;
    private String state;
    private Double longitude;
    private Double latitude;
    private String photoUrl;
    private List<String> foodPhotosUrls;
    private Boolean stayIn;
    private Boolean veg;
    private Boolean nonveg;
    private Boolean coolbar;
    
    private Integer adminRatings; // <--- ADDED HERE
    private Integer ratings; 
    
    private String phoneNumber;
    private String special;
    private String direction;

    // --- GETTERS AND SETTERS ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getExactPlace() { return exactPlace; }
    public void setExactPlace(String exactPlace) { this.exactPlace = exactPlace; }

    public String getPlace() { return place; }
    public void setPlace(String place) { this.place = place; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public List<String> getFoodPhotosUrls() { return foodPhotosUrls; }
    public void setFoodPhotosUrls(List<String> foodPhotosUrls) { this.foodPhotosUrls = foodPhotosUrls; }

    public Boolean getStayIn() { return stayIn; }
    public Boolean isStayIn() { return stayIn != null && stayIn; }
    public void setStayIn(Boolean stayIn) { this.stayIn = stayIn; }

    public Boolean getVeg() { return veg; }
    public Boolean isVeg() { return veg != null && veg; }
    public void setVeg(Boolean veg) { this.veg = veg; }

    public Boolean getNonveg() { return nonveg; }
    public Boolean isNonveg() { return nonveg != null && nonveg; }
    public void setNonveg(Boolean nonveg) { this.nonveg = nonveg; }

    public Boolean getCoolbar() { return coolbar; }
    public Boolean isCoolbar() { return coolbar != null && coolbar; }
    public void setCoolbar(Boolean coolbar) { this.coolbar = coolbar; }

    // --- ADDED GETTER & SETTER ---
    public Integer getAdminRatings() { return adminRatings != null ? adminRatings : 0; }
    public void setAdminRatings(Integer adminRatings) { this.adminRatings = adminRatings; }

    public Integer getRatings() { return ratings != null ? ratings : 0; }
    public void setRatings(Integer ratings) { this.ratings = ratings; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getSpecial() { return special; }
    public void setSpecial(String special) { this.special = special; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
}