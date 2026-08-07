package com.salofresh.service.family;

import com.salofresh.dto.family.FamilyMemberRequest;
import com.salofresh.dto.family.FamilyMemberResponse;

import java.util.List;

public interface FamilyMemberService {

    List<FamilyMemberResponse> listFamilyMembers(Long userId);

    FamilyMemberResponse getFamilyMember(Long userId, Long familyMemberId);

    FamilyMemberResponse createFamilyMember(Long userId, FamilyMemberRequest request);

    FamilyMemberResponse updateFamilyMember(Long userId, Long familyMemberId, FamilyMemberRequest request);

    void deleteFamilyMember(Long userId, Long familyMemberId);
}
