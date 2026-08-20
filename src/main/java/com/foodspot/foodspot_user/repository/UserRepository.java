package com.foodspot.foodspot_user.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.foodspot.foodspot_user.model.UserModel;

import java.util.Optional;

public interface UserRepository extends MongoRepository<UserModel, String> {
    // Custom query method used across your login and reset services
    Optional<UserModel> findByEmail(String email);
}