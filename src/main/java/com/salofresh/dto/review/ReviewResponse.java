package com.salofresh.dto.review;

import com.salofresh.common.enums.ReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private Long id;

    private Long customerId;
    private String customerName;
    private String customerAvatarUrl;

    private Long salonId;

    private Long employeeId;
    private String employeeName;

    private Integer salonRating;
    private Integer employeeRating;
    private String comment;

    private List<String> images;

    private String ownerReply;
    private Instant ownerReplyAt;

    private ReviewStatus status;
    private String reportReason;

    private Instant createdAt;
}
