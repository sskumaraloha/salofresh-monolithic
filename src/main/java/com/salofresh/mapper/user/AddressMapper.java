package com.salofresh.mapper.user;

import com.salofresh.dto.user.AddressRequest;
import com.salofresh.dto.user.AddressResponse;
import com.salofresh.entity.Address;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AddressMapper {

    @Mapping(target = "cityId", source = "city.id")
    @Mapping(target = "cityName", source = "city.name")
    @Mapping(target = "isDefault", source = "default")
    AddressResponse toResponse(Address address);

    // isDefault is intentionally ignored here: Lombok's @Builder exposes it as isDefault(boolean)
    // on Address.AddressBuilder, which MapStruct cannot reliably match against the
    // isDefault()/"default" property pair inferred from AddressRequest. It is set explicitly
    // by the service layer instead, which also enforces the "single default address" invariant.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "city", ignore = true)
    @Mapping(target = "isDefault", ignore = true)
    Address toEntity(AddressRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "city", ignore = true)
    @Mapping(target = "default", ignore = true)
    void updateEntityFromRequest(AddressRequest request, @MappingTarget Address address);
}
