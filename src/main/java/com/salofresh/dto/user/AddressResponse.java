package com.salofresh.dto.user;

import com.salofresh.common.enums.AddressType;
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
public class AddressResponse {

    private Long id;
    private AddressType addressType;
    private String addressLine1;
    private String addressLine2;
    private String landmark;
    private Long cityId;
    private String cityName;
    private String pinCode;
    private Double latitude;
    private Double longitude;
    private boolean isDefault;
}
