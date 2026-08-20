package com.foodspot.foodspot_user.repository;

import com.foodspot.foodspot_user.model.SpotModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpotRepository extends MongoRepository<SpotModel, String> {
    // Custom query method to find spots by their district field
    List<SpotModel> findByDistrict(String district);
}