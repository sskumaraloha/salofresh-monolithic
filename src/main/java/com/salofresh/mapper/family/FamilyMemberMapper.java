package com.salofresh.mapper.family;

import com.salofresh.dto.family.FamilyMemberRequest;
import com.salofresh.dto.family.FamilyMemberResponse;
import com.salofresh.entity.FamilyMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface FamilyMemberMapper {

    FamilyMemberResponse toResponse(FamilyMember familyMember);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    FamilyMember toEntity(FamilyMemberRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    void updateEntityFromRequest(FamilyMemberRequest request, @MappingTarget FamilyMember familyMember);
}
