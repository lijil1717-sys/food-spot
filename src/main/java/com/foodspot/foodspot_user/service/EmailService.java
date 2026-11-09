
package com.foodspot.foodspot_user.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class EmailService {

    @Value("${resend.api.key}")
    private String apiKey;

    @Value("${resend.from.email}")
    private String fromEmail;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public void sendVerificationEmail(String targetEmail, String numericCode) {

        try {
            Map<String, Object> emailBody = Map.of(
                "from", fromEmail,
                "to", List.of(targetEmail),
                "subject", "FoodSpot - Your 6-Digit Verification Code",
                "text", "Welcome to FoodSpot!\n\n" +
                        "Your secure registration code is: " + numericCode + "\n\n" +
                        "This code expires in 5 minutes. Please do not share this with anyone."
            );

            String jsonBody = objectMapper.writeValueAsString(emailBody);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.resend.com/emails"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

            HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                    "Resend email failed: " + response.statusCode() +
                    " - " + response.body()
                );
            }

            System.out.println("Verification email sent successfully.");

        } catch (IOException e) {
            throw new IllegalStateException("Resend API communication error", e);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Email sending interrupted", e);
        }
    }
}
