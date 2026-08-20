package com.foodspot.foodspot_user.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping; 

@Controller
public class WebController {

    // Serves the login/auth page at http://localhost:8081/foodspot
    @GetMapping("/foodspot")
    public String foodSpotPage() {
        return "foodspot"; // Looks for foodspot.html in templates/
    }

}
