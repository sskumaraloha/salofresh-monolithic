package com.salofresh.repository;

import com.salofresh.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CityRepository extends JpaRepository<City, Long> {

    List<City> findAllByStateId(Long stateId);

    List<City> findAllByPopularTrue();

    List<City> findAllByNameContainingIgnoreCase(String name);
}
