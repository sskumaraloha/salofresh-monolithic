package com.salofresh.dto.user;

import com.salofresh.common.enums.AddressType;
import com.salofresh.constant.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
public class AddressRequest {

    @NotNull(message = "Address type is required")
    private AddressType addressType;

    @NotBlank(message = "Address line 1 is required")
    @Size(max = 255, message = "Address line 1 must not exceed 255 characters")
    private String addressLine1;

    @Size(max = 255, message = "Address line 2 must not exceed 255 characters")
    private String addressLine2;

    @Size(max = 150, message = "Landmark must not exceed 150 characters")
    private String landmark;

    @NotNull(message = "City is required")
    private Long cityId;

    @Pattern(regexp = ValidationPatterns.PIN_CODE, message = "Pin code must be a valid 6-digit code")
    private String pinCode;

    private Double latitude;

    private Double longitude;

    @Builder.Default
    private boolean isDefault = false;
}
