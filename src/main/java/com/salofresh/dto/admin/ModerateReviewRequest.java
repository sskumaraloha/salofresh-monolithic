package com.salofresh.dto.admin;

import com.salofresh.common.enums.ReviewStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerateReviewRequest {

    @NotNull(message = "Status is required")
    private ReviewStatus status;
}
