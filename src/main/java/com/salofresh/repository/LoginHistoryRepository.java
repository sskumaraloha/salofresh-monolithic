package com.salofresh.repository;

import com.salofresh.entity.LoginHistory;
import com.salofresh.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {

    Page<LoginHistory> findAllByUserOrderByLoginAtDesc(User user, Pageable pageable);
}
