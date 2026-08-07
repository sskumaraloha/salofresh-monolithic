package com.salofresh.mapper.inventory;

import com.salofresh.dto.inventory.StockTransactionResponse;
import com.salofresh.entity.StockTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StockTransactionMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "performedByUserId", source = "performedBy.id")
    @Mapping(target = "performedByName", expression = "java(transaction.getPerformedBy() != null "
            + "? transaction.getPerformedBy().getFullName() : null)")
    StockTransactionResponse toResponse(StockTransaction transaction);

    List<StockTransactionResponse> toResponseList(List<StockTransaction> transactions);
}
