package com.salofresh.dto.gdpr;

import com.salofresh.common.enums.DataRequestType;
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
public class CreateDataRequestRequest {

    @NotNull(message = "Request type is required")
    private DataRequestType requestType;
}
