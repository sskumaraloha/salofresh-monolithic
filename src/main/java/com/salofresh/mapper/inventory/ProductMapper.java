package com.salofresh.mapper.inventory;

import com.salofresh.dto.inventory.ProductRequest;
import com.salofresh.dto.inventory.ProductResponse;
import com.salofresh.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "salonId", source = "salon.id")
    @Mapping(target = "belowReorderLevel", expression = "java(product.isBelowReorderLevel())")
    ProductResponse toResponse(Product product);

    List<ProductResponse> toResponseList(List<Product> products);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "salon", ignore = true)
    @Mapping(target = "currentStock", ignore = true)
    @Mapping(target = "active", ignore = true)
    Product toEntity(ProductRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "salon", ignore = true)
    @Mapping(target = "currentStock", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void updateEntityFromRequest(ProductRequest request, @MappingTarget Product product);
}
