package com.salofresh.repository;

import com.salofresh.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findAllBySalonId(Long salonId);

    Optional<Document> findByIdAndSalonId(Long id, Long salonId);
}
