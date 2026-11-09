
package com.foodspot.foodspot_user.controller;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    @Autowired
    private Cloudinary cloudinary;

    // Cloudinary folder for all new spot images
    private static final String CLOUDINARY_FOLDER = "newspots";

    // ------------------------------------------------
    // SHOW ADD SPOT PAGE
    // ------------------------------------------------

    @GetMapping("/add-spot")
    public String showAddSpotPage(
            HttpSession session,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {

        String loggedInEmail = (session != null)
                ? (String) session.getAttribute("loggedInUserEmail")
                : null;

        // Check login
        if (loggedInEmail == null) {

            boolean isAjax = "XMLHttpRequest".equals(
                    request.getHeader("X-Requested-With"));

            if (isAjax) {
                try {
                    response.sendError(
                            HttpServletResponse.SC_UNAUTHORIZED,
                            "Session expired");
                    return null;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return "redirect:/foodspot";
        }

        // Fetch user details
        Query userQuery = new Query(
                Criteria.where("email").is(loggedInEmail));

        UserModel loggedInUser = mongoTemplate.findOne(
                userQuery, UserModel.class, "User");

        if (loggedInUser != null) {
            model.addAttribute("userName", loggedInUser.getName());
            model.addAttribute("userEmail", loggedInUser.getEmail());
            model.addAttribute("userCreatedAt", loggedInUser.getCreatedAt());
        } else {
            model.addAttribute("userName", "User");
            model.addAttribute("userEmail", loggedInEmail);
        }

        // Count rated spots
        long userRatedCount = 0;

        if (loggedInUser != null && loggedInUser.getId() != null) {
            userRatedCount =
                    userRatingRepository.countByUserId(loggedInUser.getId());
        }

        model.addAttribute("userRatedCount", userRatedCount);

        // Count saved spots
        long userSavedCount = 0;

        if (loggedInUser != null && loggedInUser.getId() != null) {
            userSavedCount =
                    savedSpotRepository.countByUserId(loggedInUser.getId());
        }

        model.addAttribute("userSavedCount", userSavedCount);

        // Count shared spots
        long userSharedCount = 0;

        if (loggedInUser != null && loggedInUser.getId() != null) {
            userSharedCount =
                    newSpotRepository.countByUserId(loggedInUser.getId());
        }

        model.addAttribute("userSharedCount", userSharedCount);

        return "add-spot";
    }

    // ------------------------------------------------
    // CLOUDINARY IMAGE UPLOAD HELPER
    // ------------------------------------------------

    private String uploadImage(MultipartFile file) throws Exception {

        if (file == null || file.isEmpty()) {
            return null;
        }

        Map<?, ?> uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", CLOUDINARY_FOLDER,
                        "resource_type", "image"
                )
        );

        return (String) uploadResult.get("secure_url");
    }

    // ------------------------------------------------
    // ADD NEW SPOT
    // ------------------------------------------------

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

            @RequestParam(value = "file", required = false)
            MultipartFile coverPhoto,

            @RequestParam(value = "files", required = false)
            MultipartFile[] additionalFiles,

            HttpSession session,
            RedirectAttributes redirectAttributes) {

        // Check logged-in user
        String loggedInEmail = (session != null)
                ? (String) session.getAttribute("loggedInUserEmail")
                : null;

        if (loggedInEmail == null) {
            return "redirect:/foodspot";
        }

        // Fetch user ID
        Query userQuery = new Query(
                Criteria.where("email").is(loggedInEmail));

        UserModel loggedInUser = mongoTemplate.findOne(
                userQuery, UserModel.class, "User");

        // Create new spot
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

        // Upload images to Cloudinary
        try {

            // 1. Upload cover photo to newspots folder
            if (coverPhoto != null && !coverPhoto.isEmpty()) {

                String coverUrl = uploadImage(coverPhoto);

                newSpot.setCoverPhotoUrl(coverUrl);
            }

            // 2. Upload additional food photos to newspots folder
            List<String> additionalPhotoUrls = new ArrayList<>();

            if (additionalFiles != null) {

                for (MultipartFile file : additionalFiles) {

                    if (file != null && !file.isEmpty()) {

                        String photoUrl = uploadImage(file);

                        if (photoUrl != null) {
                            additionalPhotoUrls.add(photoUrl);
                        }
                    }
                }
            }

            newSpot.setPhotoUrls(additionalPhotoUrls);

        } catch (Exception e) {

            e.printStackTrace();

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Image upload failed. Please try again."
            );

            return "redirect:/add-spot";
        }

        // 3. Save spot details and Cloudinary URLs in MongoDB
        newSpotRepository.save(newSpot);

        // Success message
        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Spot has been submitted successfully! After the review by admin, the spot will be added to the main page."
        );

        return "redirect:/add-spot";
    }
}
