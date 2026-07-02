package com.salofresh.mapper.payment;

import com.salofresh.dto.payment.TransactionResponse;
import com.salofresh.entity.Transaction;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    TransactionResponse toResponse(Transaction transaction);
}
