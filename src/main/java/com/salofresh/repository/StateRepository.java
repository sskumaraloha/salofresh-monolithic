package com.salofresh.repository;

import com.salofresh.entity.State;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StateRepository extends JpaRepository<State, Long> {

    List<State> findAllByCountryId(Long countryId);
}
