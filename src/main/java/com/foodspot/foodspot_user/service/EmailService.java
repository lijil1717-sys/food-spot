package com.foodspot.foodspot_user.service; // <-- Changed 'Service' to lowercase 'service'

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service 
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendVerificationEmail(String targetEmail, String numericCode) {
        SimpleMailMessage emailLayout = new SimpleMailMessage();
        
        emailLayout.setTo(targetEmail);
        emailLayout.setSubject("FoodSpot - Your 6-Digit Verification Code");
        emailLayout.setText(
            "Welcome to FoodSpot!\n\n" +
            "Your secure registration code is: " + numericCode + "\n\n" +
            "This code expires in 5 minutes. Please do not share this with anyone."
        );
        
        mailSender.send(emailLayout);
    }
}