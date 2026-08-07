package com.salofresh.mapper.giftcard;

import com.salofresh.dto.giftcard.GiftCardResponse;
import com.salofresh.entity.GiftCard;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GiftCardMapper {

    GiftCardResponse toResponse(GiftCard giftCard);
}
