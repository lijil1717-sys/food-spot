package com.foodspot.foodspot_user.controller;

import com.foodspot.foodspot_user.model.NewSpotModel;
import com.foodspot.foodspot_user.model.UserModel;
import com.foodspot.foodspot_user.repository.NewSpotRepository;
import com.foodspot.foodspot_user.repository.SavedSpotRepository;
import com.foodspot.foodspot_user.repository.UserRatingRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
public class AddSpotController {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private UserRatingRepository userRatingRepository;

    @Autowired
    private SavedSpotRepository savedSpotRepository;

    @Autowired
    private NewSpotRepository newSpotRepository;

    private static final String UPLOAD_DIR = "src/main/resources/static/uploads/";

    @GetMapping("/add-spot")
    public String showAddSpotPage(
            HttpSession session,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {

        // 1. Fetch User details from MongoDB using email stored in session
        String loggedInEmail = (session != null) ? (String) session.getAttribute("loggedInUserEmail") : null;
        
        // ---> SECURITY CHECK: Redirect if user is not logged in <---
        if (loggedInEmail == null) {
            boolean isAjax = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
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

        // 2. Count how many spots this user has rated using userId
        long userRatedCount = 0;
        if (loggedInUser != null && loggedInUser.getId() != null) {
            userRatedCount = userRatingRepository.countByUserId(loggedInUser.getId());
        }
        model.addAttribute("userRatedCount", userRatedCount);

        // 3. Count how many spots this user has saved using userId
        long userSavedCount = 0;
        if (loggedInUser != null && loggedInUser.getId() != null) {
            userSavedCount = savedSpotRepository.countByUserId(loggedInUser.getId());
        }
        model.addAttribute("userSavedCount", userSavedCount);

        // 4. Count how many spots this user has shared using userId
        long userSharedCount = 0;
        if (loggedInUser != null && loggedInUser.getId() != null) {
            userSharedCount = newSpotRepository.countByUserId(loggedInUser.getId());
        }
        model.addAttribute("userSharedCount", userSharedCount);

        return "add-spot"; // Renders add-spot.html with full profile attributes
    }

    @PostMapping("/api/spots")
    public String handleAddSpotSubmission(
            @RequestParam("name") String name,
            @RequestParam(value = "place", required = false) String place,
            @RequestParam("district") String district,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "categories", required = false) List<String> categories,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "speciality", required = false) String speciality,
            @RequestParam(value = "mapLink", required = false) String mapLink,
            @RequestParam(value = "directions", required = false) String directions,
            @RequestParam(value = "file", required = false) MultipartFile coverPhoto,
            @RequestParam(value = "files", required = false) MultipartFile[] additionalFiles,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        String loggedInEmail = (session != null) ? (String) session.getAttribute("loggedInUserEmail") : null;
        if (loggedInEmail == null) {
            return "redirect:/foodspot";
        }

        // Fetch user ID to bind submission to the author
        Query userQuery = new Query(Criteria.where("email").is(loggedInEmail));
        UserModel loggedInUser = mongoTemplate.findOne(userQuery, UserModel.class, "User");

        NewSpotModel newSpot = new NewSpotModel();
        if (loggedInUser != null) {
            newSpot.setUserId(loggedInUser.getId());
        }

        // Map form attributes
        newSpot.setName(name);
        newSpot.setPlace(place);
        newSpot.setDistrict(district);
        newSpot.setState(state);
        newSpot.setCategories(categories);
        newSpot.setPhone(phone);
        newSpot.setSpeciality(speciality);
        newSpot.setMapLink(mapLink);
        newSpot.setDirections(directions);

        // Process File uploads
        try {
            File directory = new File(UPLOAD_DIR);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Cover photo handler
            if (coverPhoto != null && !coverPhoto.isEmpty()) {
                String coverName = "cover_" + UUID.randomUUID().toString() + "_" + coverPhoto.getOriginalFilename();
                Path filePath = Paths.get(UPLOAD_DIR + coverName);
                Files.write(filePath, coverPhoto.getBytes());
                newSpot.setCoverPhotoUrl("/uploads/" + coverName);
            }

            // Additional food photos handler
            List<String> additionalPhotoUrls = new ArrayList<>();
            if (additionalFiles != null && additionalFiles.length > 0) {
                for (MultipartFile file : additionalFiles) {
                    if (!file.isEmpty()) {
                        String uniqueName = "food_" + UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                        Path filePath = Paths.get(UPLOAD_DIR + uniqueName);
                        Files.write(filePath, file.getBytes());
                        additionalPhotoUrls.add("/uploads/" + uniqueName);
                    }
                }
            }
            newSpot.setPhotoUrls(additionalPhotoUrls);

        } catch (IOException e) {
            e.printStackTrace();
        }

        // Save entry into 'new_spots' collection
        newSpotRepository.save(newSpot);

        // Flash message and redirect back to the add-spot page
        redirectAttributes.addFlashAttribute("successMessage", "Spot has been submitted successfully!");
        return "redirect:/add-spot";
    }
}