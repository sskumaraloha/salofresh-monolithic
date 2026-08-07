package com.salofresh.repository;

import com.salofresh.entity.ServiceProductUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceProductUsageRepository extends JpaRepository<ServiceProductUsage, Long> {

    List<ServiceProductUsage> findAllByServiceId(Long serviceId);

    void deleteAllByServiceId(Long serviceId);
}
