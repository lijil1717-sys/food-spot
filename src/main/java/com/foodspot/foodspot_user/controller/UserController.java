package com.foodspot.foodspot_user.controller;

import com.foodspot.foodspot_user.model.SpotModel;
import com.foodspot.foodspot_user.model.UserModel;
import com.foodspot.foodspot_user.model.UserRatingModel;
import com.foodspot.foodspot_user.repository.*;
import com.foodspot.foodspot_user.service.EmailService;
import com.foodspot.foodspot_user.util.EncryptionUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRatingRepository userRatingRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private MongoTemplate mongoTemplate;

    // ---> Helper class to manage 5-minute code expiration <---
    public static class VerificationToken {
        private String code;
        private LocalDateTime expiryTime;

        public VerificationToken(String code, int minutesValid) {
            this.code = code;
            this.expiryTime = LocalDateTime.now().plusMinutes(minutesValid);
        }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiryTime);
        }

        public String getCode() {
            return code;
        }
    }

    private final Map<String, VerificationToken> verificationCodes = new ConcurrentHashMap<>();
    private final Map<String, String> pendingUserNames = new ConcurrentHashMap<>();

    @PostMapping("/login")
    public ResponseEntity<String> loginUser(@RequestBody Map<String, String> loginData, HttpServletRequest request) {
        String email = loginData.get("email");
        String password = loginData.get("password");

        Optional<UserModel> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isPresent()) {
            String storedPassword = userOpt.get().getPassword();
            String decryptedPassword = "";
            
            try {
                decryptedPassword = EncryptionUtil.decrypt(storedPassword);
            } catch (Exception e) {
                decryptedPassword = storedPassword;
            }
            
            if (decryptedPassword != null && decryptedPassword.equals(password)) {
                HttpSession session = request.getSession(true);
                session.setAttribute("loggedInUserEmail", email);
                
                return ResponseEntity.ok("Login successful!");
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password.");
    }

    /**
     * STEP 1: INLINE VERIFY BUTTON CLICK (SENDS OTP ONLY)
     */
    @PostMapping("/register-request")
    public ResponseEntity<String> requestRegistration(@RequestBody Map<String, String> regData) {
        String name = regData.get("name");
        String email = regData.get("email");

        if (name == null || email == null || email.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Name and email are required.");
        }

        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email ID is already registered.");
        }

        String sixDigitCode = String.format("%06d", new Random().nextInt(1000000));

        // Store code with a strict 5-minute validity window
        verificationCodes.put(email, new VerificationToken(sixDigitCode, 5));
        pendingUserNames.put(email, name);

        try {
            emailService.sendVerificationEmail(email, sixDigitCode);
            return ResponseEntity.ok("Verification code sent to your email inbox.");
        } catch (Exception e) {
            verificationCodes.remove(email);
            pendingUserNames.remove(email);
            
            e.printStackTrace(); 
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Mail Server Error: " + e.getMessage());
        }
    }

    /**
     * STEP 2: FINAL REGISTER BUTTON SUBMISSION
     */
    @PostMapping("/verify-code")
    public ResponseEntity<String> verifyRegistrationCode(@RequestBody Map<String, String> requestData) {
        System.out.println("📥 RECEIVED DATA MAP AT VERIFY: " + requestData);

        String email = requestData.get("email");
        String enteredCode = requestData.get("verificationCode");
        String finalName = requestData.get("name");
        String finalPassword = requestData.get("password");

        System.out.println("🔍 Parsed Values -> Name: " + finalName + " | Password: " + finalPassword);

        VerificationToken tokenData = verificationCodes.get(email);

        if (tokenData == null) {
            return ResponseEntity.badRequest().body("Error: Verification session timed out or expired.");
        }

        // ---> CHECK IF CODE EXPIRED AFTER 5 MINUTES <---
        if (tokenData.isExpired()) {
            verificationCodes.remove(email);
            pendingUserNames.remove(email);
            return ResponseEntity.badRequest().body("Error: Verification code has expired after 5 minutes. Please request a new one.");
        }

        if (!tokenData.getCode().equals(enteredCode)) {
            return ResponseEntity.badRequest().body("Error: Invalid 6-digit verification code.");
        }

        if (finalName == null || finalName.trim().isEmpty()) {
            finalName = pendingUserNames.get(email);
        }

        String encryptedPassword = EncryptionUtil.encrypt(finalPassword);

        UserModel newUser = new UserModel();
        newUser.setName(finalName);
        newUser.setEmail(email);
        newUser.setPassword(encryptedPassword);
        
        // ---> STORE CURRENT TIME AND DATE <---
        newUser.setCreatedAt(LocalDateTime.now());

        System.out.println("💾 SAVING TO MONGODB -> Name: " + newUser.getName() + ", Email: " + newUser.getEmail() + ", CreatedAt: " + newUser.getCreatedAt());

        userRepository.save(newUser);

        verificationCodes.remove(email);
        pendingUserNames.remove(email);

        return ResponseEntity.status(HttpStatus.CREATED).body("Account created successfully!");
    }

    /**
     * PANEL 3 (STEP 1): REQUEST PASSWORD RESET OTP
     */
    @PostMapping("/forgot-password-request")
    public ResponseEntity<String> requestPasswordReset(@RequestBody Map<String, String> requestData) {
        String email = requestData.get("email");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email is required.");
        }

        Optional<UserModel> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found with this email ID.");
        }

        String sixDigitCode = String.format("%06d", new Random().nextInt(1000000));

        // Store code with a strict 5-minute validity window (reusing your verificationCodes map)
        verificationCodes.put(email, new VerificationToken(sixDigitCode, 5));

        try {
            // You can use a dedicated email method or your verification email method
            emailService.sendVerificationEmail(email, sixDigitCode);
            return ResponseEntity.ok("Password reset verification code sent to your email inbox.");
        } catch (Exception e) {
            verificationCodes.remove(email);
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Mail Server Error: " + e.getMessage());
        }
    }

    /**
     * PANEL 3 (STEP 2): VERIFY OTP AND UPDATE PASSWORD
     */
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody Map<String, String> resetData) {
        String email = resetData.get("email");
        String enteredCode = resetData.get("verificationCode");
        String newPassword = resetData.get("newPassword");
        String confirmPassword = resetData.get("confirmPassword");

        if (email == null || enteredCode == null || newPassword == null || confirmPassword == null) {
            return ResponseEntity.badRequest().body("All fields are required.");
        }

        if (!newPassword.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body("Passwords do not match.");
        }

        VerificationToken tokenData = verificationCodes.get(email);

        if (tokenData == null) {
            return ResponseEntity.badRequest().body("Error: Verification session timed out or expired.");
        }

        // ---> CHECK IF CODE EXPIRED AFTER 5 MINUTES <---
        if (tokenData.isExpired()) {
            verificationCodes.remove(email);
            return ResponseEntity.badRequest().body("Error: Verification code has expired after 5 minutes. Please request a new one.");
        }

        if (!tokenData.getCode().equals(enteredCode)) {
            return ResponseEntity.badRequest().body("Error: Invalid 6-digit verification code.");
        }

        Optional<UserModel> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found with this email ID.");
        }

        String encryptedNewPassword = EncryptionUtil.encrypt(newPassword);

        UserModel user = userOpt.get();
        user.setPassword(encryptedNewPassword);
        userRepository.save(user); 

        // Clean up the code after successful use
        verificationCodes.remove(email);

        return ResponseEntity.ok("Password updated successfully!");
    }
    /**
     * PANEL 4: RETRIEVE FOOD SPOTS ALONG ACTIVE NAVIGATION PATH LINE
     */
    @PostMapping("/food-spots-along-route")
    public ResponseEntity<List<SpotModel>> getFoodSpotsAlongRoute(@RequestBody Map<String, List<List<Double>>> requestBody) {
        List<List<Double>> coordinates = requestBody.get("coordinates");
        
        if (coordinates == null || coordinates.isEmpty()) {
            return ResponseEntity.ok(new ArrayList<>());
        }

        double toleranceDegree = 0.0045; 
        List<Criteria> routeSegmentCriteriaList = new ArrayList<>();

        for (int i = 0; i < coordinates.size(); i++) {
            if (i % 7 == 0) {
                List<Double> point = coordinates.get(i);
                double routeLng = point.get(0); 
                double routeLat = point.get(1); 

                Criteria boundingBox = Criteria.where("longitude")
                        .gte(routeLng - toleranceDegree).lte(routeLng + toleranceDegree)
                        .and("latitude")
                        .gte(routeLat - toleranceDegree).lte(routeLat + toleranceDegree);

                routeSegmentCriteriaList.add(boundingBox);
            }
        }

        Query finalRouteQuery = new Query();
        finalRouteQuery.addCriteria(new Criteria().orOperator(routeSegmentCriteriaList.toArray(new Criteria[0])));

        List<SpotModel> matchingSpots = mongoTemplate.find(finalRouteQuery, SpotModel.class);

        return ResponseEntity.ok(matchingSpots);
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/all-food-spots")
    public ResponseEntity<?> getAllFoodSpots() {
        try {
            List<SpotModel> allSpots = mongoTemplate.findAll(SpotModel.class);
            System.out.println("📦 Successfully retrieved " + allSpots.size() + " spots from MongoDB.");
            return ResponseEntity.ok(allSpots);
        } catch (Exception e) {
            System.err.println("❌ MongoDB Fetch Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Database Connection Failed: " + e.getMessage());
        }
    }

/**
     * SUBMIT REVIEW AND UPDATE SPOT RATINGS COLLECTION (CUMULATIVE RUNNING SUM)
     */
    @PostMapping("/submit-review")
    @ResponseBody
    public ResponseEntity<?> submitReview(@RequestBody UserRatingModel request, HttpSession session) {
        try {
            String loggedInEmail = (session != null) ? (String) session.getAttribute("loggedInUserEmail") : null;
            if (loggedInEmail == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "You must be logged in to submit a review."));
            }

            Query userQuery = new Query(Criteria.where("email").is(loggedInEmail));
            UserModel loggedInUser = mongoTemplate.findOne(userQuery, UserModel.class, "User");
            if (loggedInUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "User account not found."));
            }

            if (request.getSpotId() == null || request.getSpotId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Spot ID is required."));
            }

            SpotModel spot = mongoTemplate.findById(request.getSpotId(), SpotModel.class);
            if (spot == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Spot not found."));
            }

            int ratingValue = (request.getRatings() != null) ? request.getRatings() : 0;
            if (ratingValue < 1 || ratingValue > 5) {
                return ResponseEntity.badRequest().body(Map.of("message", "Rating must be between 1 and 5."));
            }

            // Check if user already reviewed this spot
            Optional<UserRatingModel> existingReviewOpt = userRatingRepository.findBySpotIdAndUserId(request.getSpotId(), loggedInUser.getId());

            int currentSpotRatingsSum = (spot.getRatings() != null) ? spot.getRatings() : 0;
            int newTotalSum;

            UserRatingModel ratingToSave;
            if (existingReviewOpt.isPresent()) {
                ratingToSave = existingReviewOpt.get();
                int oldUserRating = (ratingToSave.getRatings() != null) ? ratingToSave.getRatings() : 0;
                
                ratingToSave.setRatings(ratingValue);
                ratingToSave.setReview(request.getReview());
                
                // Adjust sum: subtract previous review value, add new review value
                newTotalSum = currentSpotRatingsSum - oldUserRating + ratingValue;
            } else {
                ratingToSave = new UserRatingModel();
                ratingToSave.setSpotId(request.getSpotId());
                ratingToSave.setUserId(loggedInUser.getId());
                ratingToSave.setRatings(ratingValue);
                ratingToSave.setReview(request.getReview());
                
                // Add new review value directly to total sum
                newTotalSum = currentSpotRatingsSum + ratingValue;
            }
            
            // Explicitly persist the individual user review
            userRatingRepository.save(ratingToSave);

            // Fetch count of regular users who rated this spot
            List<UserRatingModel> allSpotRatings = userRatingRepository.findBySpotId(request.getSpotId());
            int regularUserCount = allSpotRatings.size(); 

            // Include admin as a base contributor: Total users = regular users + 1
            int totalUsers = regularUserCount + 1; 

            // Save cumulative total sum back to the spot's ratings field
            spot.setRatings(newTotalSum);
            mongoTemplate.save(spot);

            // Safely retrieve adminRatings using model getter
            int adminRatings = spot.getAdminRatings();

            // Calculate true average including the admin base contributor
            double trueAverage = (double) (adminRatings + newTotalSum) / totalUsers;
            trueAverage = Math.round(trueAverage * 100.0) / 100.0;

            // Console log to check the values
            System.out.println("total sum = " + (adminRatings + newTotalSum));
            System.out.println("total users = " + totalUsers);

            long updatedRatedCount = userRatingRepository.countByUserId(loggedInUser.getId());

            return ResponseEntity.ok(Map.of(
                "message", existingReviewOpt.isPresent() ? "Review updated successfully!" : "Review submitted successfully!",
                "ratedCount", updatedRatedCount,
                "newCount", totalUsers,
                "newAverage", trueAverage,
                "newTotalSum", newTotalSum
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Server Error: " + e.getMessage()));
        }
    }
    @PostMapping("/logout")
    public ResponseEntity<String> logoutUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate(); 
        }
        return ResponseEntity.ok("Logged out successfully!");
    }

    @GetMapping("/get-user-review")
    public ResponseEntity<?> getUserReview(@RequestParam("spotId") String spotId, HttpSession session) {
        String loggedInEmail = (session != null) ? (String) session.getAttribute("loggedInUserEmail") : null;
        if (loggedInEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not logged in");
        }

        Query userQuery = new Query(Criteria.where("email").is(loggedInEmail));
        UserModel loggedInUser = mongoTemplate.findOne(userQuery, UserModel.class, "User");
        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }

        Optional<UserRatingModel> existingReview = userRatingRepository.findBySpotIdAndUserId(spotId, loggedInUser.getId());
        
        if (existingReview.isPresent()) {
            return ResponseEntity.ok(existingReview.get());
        } else {
            return ResponseEntity.noContent().build(); 
        }
    }
}