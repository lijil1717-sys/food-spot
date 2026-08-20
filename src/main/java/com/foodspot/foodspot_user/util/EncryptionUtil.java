package com.foodspot.foodspot_user.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class EncryptionUtil {
    private static final String SECRET_KEY = "FoodSpotSecret12"; // Exactly 16 bytes
    private static final String ALGORITHM = "AES/ECB/PKCS5Padding"; // Explicit transformation

    public static String encrypt(String plainText) {
        if (plainText == null) return null;
        try {
            SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error occurred during text encryption", e);
        }
    }

    public static String decrypt(String encryptedText) {
        if (encryptedText == null) return null;
        try {
            SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decryptedBytes);
        } catch (Exception e) {
            // SAFE FALLBACK: If decryption fails (e.g., data was stored as plain text), 
            // return the raw string instead of throwing a 500 server error.
            return encryptedText;
        }
    }
}