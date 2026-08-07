package com.salofresh.dto.family;

import com.salofresh.common.enums.FamilyRelationship;
import com.salofresh.common.enums.Gender;
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
public class FamilyMemberResponse {

    private Long id;
    private String name;
    private FamilyRelationship relationship;
    private Gender gender;
    private LocalDate dateOfBirth;
    private String phone;
}
