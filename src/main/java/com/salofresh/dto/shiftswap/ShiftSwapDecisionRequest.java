package com.salofresh.dto.shiftswap;

import jakarta.validation.constraints.NotNull;
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
public class ShiftSwapDecisionRequest {

    @NotNull(message = "Approve flag is required")
    private Boolean approve;

    @Size(max = 500, message = "Note must not exceed 500 characters")
    private String note;
}
