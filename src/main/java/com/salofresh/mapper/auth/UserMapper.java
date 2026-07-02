package com.salofresh.mapper.auth;

import com.salofresh.dto.auth.UserSummaryResponse;
import com.salofresh.entity.Role;
import com.salofresh.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roles", source = "roles", qualifiedByName = "rolesToNames")
    UserSummaryResponse toSummary(User user);

    @org.mapstruct.Named("rolesToNames")
    default Set<String> rolesToNames(Set<Role> roles) {
        if (roles == null) {
            return Set.of();
        }
        return roles.stream().map(role -> role.getName().name()).collect(Collectors.toSet());
    }
}
