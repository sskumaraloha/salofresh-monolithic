package com.salofresh.repository;

import com.salofresh.entity.Address;
import com.salofresh.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findAllByUserAndDeletedFalse(User user);

    Optional<Address> findByIdAndUserAndDeletedFalse(Long id, User user);

    Optional<Address> findByUserAndIsDefaultTrueAndDeletedFalse(User user);
}
