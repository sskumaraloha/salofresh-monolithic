package com.salofresh.service.impl.family;

import com.salofresh.constant.ValidationPatterns;
import com.salofresh.dto.family.FamilyMemberRequest;
import com.salofresh.dto.family.FamilyMemberResponse;
import com.salofresh.entity.FamilyMember;
import com.salofresh.entity.User;
import com.salofresh.exception.BadRequestException;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.mapper.family.FamilyMemberMapper;
import com.salofresh.repository.FamilyMemberRepository;
import com.salofresh.repository.UserRepository;
import com.salofresh.service.family.FamilyMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class FamilyMemberServiceImpl implements FamilyMemberService {

    private static final Pattern PHONE_PATTERN = Pattern.compile(ValidationPatterns.PHONE);

    private final FamilyMemberRepository familyMemberRepository;
    private final UserRepository userRepository;
    private final FamilyMemberMapper familyMemberMapper;

    @Override
    @Transactional(readOnly = true)
    public List<FamilyMemberResponse> listFamilyMembers(Long userId) {
        return familyMemberRepository.findAllByUserIdAndDeletedFalse(userId).stream()
                .map(familyMemberMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FamilyMemberResponse getFamilyMember(Long userId, Long familyMemberId) {
        return familyMemberMapper.toResponse(getOwnedFamilyMember(userId, familyMemberId));
    }

    @Override
    @Transactional
    public FamilyMemberResponse createFamilyMember(Long userId, FamilyMemberRequest request) {
        validatePhone(request.getPhone());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        FamilyMember familyMember = familyMemberMapper.toEntity(request);
        familyMember.setUser(user);

        FamilyMember saved = familyMemberRepository.save(familyMember);
        return familyMemberMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public FamilyMemberResponse updateFamilyMember(Long userId, Long familyMemberId, FamilyMemberRequest request) {
        validatePhone(request.getPhone());
        FamilyMember familyMember = getOwnedFamilyMember(userId, familyMemberId);
        familyMemberMapper.updateEntityFromRequest(request, familyMember);

        FamilyMember saved = familyMemberRepository.save(familyMember);
        return familyMemberMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteFamilyMember(Long userId, Long familyMemberId) {
        FamilyMember familyMember = getOwnedFamilyMember(userId, familyMemberId);
        familyMember.setDeleted(true);
        familyMemberRepository.save(familyMember);
    }

    private FamilyMember getOwnedFamilyMember(Long userId, Long familyMemberId) {
        return familyMemberRepository.findByIdAndUserIdAndDeletedFalse(familyMemberId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Family member", "id", familyMemberId));
    }

    private void validatePhone(String phone) {
        if (phone != null && !phone.isBlank() && !PHONE_PATTERN.matcher(phone).matches()) {
            throw new BadRequestException("Phone number must be a valid 10-digit Indian mobile number");
        }
    }
}
