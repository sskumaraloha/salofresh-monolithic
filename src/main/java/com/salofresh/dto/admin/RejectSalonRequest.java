package com.salofresh.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class RejectSalonRequest {

    @NotBlank(message = "Rejection reason is required")
    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}
