package com.salofresh.dto.family;

import com.salofresh.common.enums.FamilyRelationship;
import com.salofresh.common.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyMemberRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @NotNull(message = "Relationship is required")
    private FamilyRelationship relationship;

    private Gender gender;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    /**
     * Optional. When supplied it must be a valid 10-digit Indian mobile number. Deliberately not
     * annotated with {@code @ValidPhone} here: {@code PhoneValidator} (existing, not modified)
     * treats a null/blank value as invalid rather than passing it through, which would make this
     * field mandatory instead of optional. The pattern is enforced conditionally - only when a
     * value is actually supplied - in FamilyMemberServiceImpl instead.
     */
    @Size(max = 15, message = "Phone number is too long")
    private String phone;
}
