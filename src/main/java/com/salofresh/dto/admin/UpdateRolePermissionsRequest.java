package com.salofresh.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRolePermissionsRequest {

    @NotNull(message = "Permission IDs are required")
    private List<Long> permissionIds;
}
