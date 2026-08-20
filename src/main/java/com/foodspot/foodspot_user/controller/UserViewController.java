package com.foodspot.foodspot_user.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.foodspot.foodspot_user.model.SavedSpotModel;
import com.foodspot.foodspot_user.model.SpotModel;
import com.foodspot.foodspot_user.model.UserModel;
import com.foodspot.foodspot_user.model.UserRatingModel;
import com.foodspot.foodspot_user.repository.NewSpotRepository;
import com.foodspot.foodspot_user.repository.SavedSpotRepository;
import com.foodspot.foodspot_user.repository.UserRatingRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class UserViewController {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private UserRatingRepository userRatingRepository;

    @Autowired
    private SavedSpotRepository savedSpotRepository;

    @Autowired
    private NewSpotRepository newSpotRepository;

    @GetMapping({"/home", "/foodspothome"})
    public String index(
            @RequestParam(value = "district", required = false) String district,
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "ajax", required = false) Boolean ajax,
            HttpSession session,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {
    
        // Fetch User details from MongoDB using the email stored in the session
        String loggedInEmail = (session != null) ? (String) session.getAttribute("loggedInUserEmail") : null;
        
        // ---> SECURITY CHECK: If user is not logged in <---
        if (loggedInEmail == null) {
            boolean isAjax = (ajax != null && ajax) || "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
            if (isAjax) {
                try {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Session expired");
                    return null;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return "redirect:/foodspot"; 
        }
        
        Query userQuery = new Query(Criteria.where("email").is(loggedInEmail));
        UserModel loggedInUser = mongoTemplate.findOne(userQuery, UserModel.class, "User");
        
        if (loggedInUser != null) {
            model.addAttribute("userName", loggedInUser.getName());
            model.addAttribute("userEmail", loggedInUser.getEmail());
            model.addAttribute("userCreatedAt", loggedInUser.getCreatedAt());
        } else {
            model.addAttribute("userName", "User");
            model.addAttribute("userEmail", loggedInEmail);
        }

        // ---> Count how many spots this user has rated using userId <---
        long userRatedCount = 0;
        if (loggedInUser != null && loggedInUser.getId() != null) {
            userRatedCount = userRatingRepository.countByUserId(loggedInUser.getId());
        }
        model.addAttribute("userRatedCount", userRatedCount);

        // ---> Count how many spots this user has saved using userId <---
        long userSavedCount = 0;
        if (loggedInUser != null && loggedInUser.getId() != null) {
            userSavedCount = savedSpotRepository.countByUserId(loggedInUser.getId());
        }
        model.addAttribute("userSavedCount", userSavedCount);

        // ---> Count how many spots this user has shared using userId <---
        long userSharedCount = 0;
        if (loggedInUser != null && loggedInUser.getId() != null) {
            userSharedCount = newSpotRepository.countByUserId(loggedInUser.getId());
        }
        model.addAttribute("userSharedCount", userSharedCount);

        Query query = new Query();

        if (district != null && !district.trim().isEmpty() && !district.equalsIgnoreCase("all")) {
            query.addCriteria(Criteria.where("district").is(district));
        } else {
            district = null; 
        }

        if (filter != null && !filter.trim().isEmpty() && !filter.equalsIgnoreCase("all")) {
            if (filter.equalsIgnoreCase("stayin")) {
                query.addCriteria(Criteria.where("stayIn").is(true));
            } else if (filter.equalsIgnoreCase("nonveg")) {
                query.addCriteria(Criteria.where("nonveg").is(true));
            } else if (filter.equalsIgnoreCase("veg")) {
                query.addCriteria(Criteria.where("veg").is(true));
            } else if (filter.equalsIgnoreCase("coolbar")) {
                query.addCriteria(Criteria.where("coolbar").is(true));
            } else if (filter.equalsIgnoreCase("saved")) {
                if (loggedInUser != null) {
                    List<SavedSpotModel> userSaves = savedSpotRepository.findByUserId(loggedInUser.getId());
                    List<String> savedSpotIds = userSaves.stream().map(SavedSpotModel::getSpotId).collect(Collectors.toList());
                    query.addCriteria(Criteria.where("_id").in(savedSpotIds.isEmpty() ? List.of("none") : savedSpotIds));
                }
            }
        } else {
            filter = null;
        }

        List<SpotModel> spots = mongoTemplate.find(query, SpotModel.class, "Spots");

        // ---> Fetch saved spot IDs for current user to render bookmark states <---
        List<String> userSavedSpotIds = Collections.emptyList();
        if (loggedInUser != null && loggedInUser.getId() != null) {
            userSavedSpotIds = savedSpotRepository.findByUserId(loggedInUser.getId())
                .stream().map(SavedSpotModel::getSpotId).collect(Collectors.toList());
        }

        // ---> ULTRA-OPTIMIZED: Fetch ratings ONLY for the spots currently displayed <---
        List<String> spotIds = spots.stream()
            .map(SpotModel::getId)
            .collect(Collectors.toList());

        List<UserRatingModel> relevantRatings = spotIds.isEmpty() ? Collections.emptyList() : userRatingRepository.findBySpotIdIn(spotIds);

        Map<String, List<UserRatingModel>> ratingsBySpotId = relevantRatings.stream()
            .collect(Collectors.groupingBy(UserRatingModel::getSpotId));

        // ---> FETCH CURRENT USER'S INDIVIDUAL REVIEWS FOR DISPLAY ON FORMS <---
        Map<String, UserRatingModel> userReviewsMap = new HashMap<>();
        if (loggedInUser != null && loggedInUser.getId() != null && !spotIds.isEmpty()) {
            List<UserRatingModel> myReviews = userRatingRepository.findByUserIdAndSpotIdIn(loggedInUser.getId(), spotIds);
            for (UserRatingModel review : myReviews) {
                userReviewsMap.put(review.getSpotId(), review);
            }
        }

        Map<String, Double> spotAverages = new HashMap<>();
        Map<String, Integer> spotCounts = new HashMap<>();

        for (SpotModel spot : spots) {
            int adminRatings = spot.getAdminRatings();
            double userRatingsSum = (spot.getRatings() != null) ? spot.getRatings() : 0.0;
            double totalSum = adminRatings + userRatingsSum;

            List<UserRatingModel> userReviews = ratingsBySpotId.getOrDefault(spot.getId(), Collections.emptyList());
            int totalUsers = userReviews.size() + 1; 

            double averageRating = 0.0;
            if (totalUsers > 0) {
                averageRating = totalSum / totalUsers;
                averageRating = Math.round(averageRating * 100.0) / 100.0;
            }

            spotAverages.put(spot.getId(), averageRating);
            spotCounts.put(spot.getId(), totalUsers);
        }

        // ---> FILTER TOP SPOTS BASED ON AVERAGE RATING (4.0 TO 5.0) <---
        if (filter != null && filter.equalsIgnoreCase("top")) {
            spots = spots.stream()
                .filter(spot -> {
                    double avg = spotAverages.getOrDefault(spot.getId(), 0.0);
                    return avg >= 4.0 && avg <= 5.0;
                })
                .collect(Collectors.toList());
        }

        model.addAttribute("spots", spots);
        model.addAttribute("spotAverages", spotAverages);
        model.addAttribute("spotCounts", spotCounts);
        model.addAttribute("userSavedSpotIds", userSavedSpotIds);
        model.addAttribute("userReviewsMap", userReviewsMap); // <--- Passed to Thymeleaf fragment
        model.addAttribute("selectedDistrict", district);
        model.addAttribute("selectedFilter", filter);

        // Auto-detect AJAX request header or explicit parameter
        boolean isAjaxRequest = (ajax != null && ajax) || "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
        if (isAjaxRequest) {
            return "foodspothome :: spotSectionFragment";
        }
        
        return "foodspothome";
    }

    // ---> TOGGLE SAVE/UNSAVE AJAX ENDPOINT <---
    @PostMapping("/api/auth/toggle-save")
    @ResponseBody
    public ResponseEntity<?> toggleSaveSpot(@RequestBody Map<String, String> request, HttpSession session) {
        String loggedInEmail = (session != null) ? (String) session.getAttribute("loggedInUserEmail") : null;
        if (loggedInEmail == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        Query userQuery = new Query(Criteria.where("email").is(loggedInEmail));
        UserModel loggedInUser = mongoTemplate.findOne(userQuery, UserModel.class, "User");
        if (loggedInUser == null) return ResponseEntity.status(404).body("User not found");

        String spotId = request.get("spotId");
        if (spotId == null) return ResponseEntity.badRequest().body("Spot ID missing");

        Optional<SavedSpotModel> existing = savedSpotRepository.findByUserIdAndSpotId(loggedInUser.getId(), spotId);
        boolean isSaved;
        if (existing.isPresent()) {
            savedSpotRepository.delete(existing.get());
            isSaved = false;
        } else {
            savedSpotRepository.save(new SavedSpotModel(loggedInUser.getId(), spotId));
            isSaved = true;
        }

        // Recalculate live total saved count for the user
        long updatedSavedCount = savedSpotRepository.countByUserId(loggedInUser.getId());

        return ResponseEntity.ok(Map.of("saved", isSaved, "savedCount", updatedSavedCount));
    }
}