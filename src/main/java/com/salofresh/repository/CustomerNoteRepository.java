package com.salofresh.repository;

import com.salofresh.entity.CustomerNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerNoteRepository extends JpaRepository<CustomerNote, Long> {

    Page<CustomerNote> findAllBySalonIdAndCustomerIdAndDeletedFalseOrderByCreatedAtDesc(
            Long salonId, Long customerId, Pageable pageable);

    Optional<CustomerNote> findByIdAndSalonIdAndDeletedFalse(Long id, Long salonId);
}
