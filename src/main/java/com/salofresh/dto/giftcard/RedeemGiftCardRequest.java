package com.salofresh.dto.giftcard;

import jakarta.validation.constraints.NotBlank;
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
public class RedeemGiftCardRequest {

    @NotBlank(message = "Gift card code is required")
    private String code;
}
