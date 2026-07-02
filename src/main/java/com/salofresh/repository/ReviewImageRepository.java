package com.salofresh.repository;

import com.salofresh.entity.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {

    List<ReviewImage> findAllByReviewId(Long reviewId);

    List<ReviewImage> findAllByReviewIdIn(List<Long> reviewIds);
}
