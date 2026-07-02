package com.salofresh.mapper.salon;

import com.salofresh.dto.salon.GalleryImageResponse;
import com.salofresh.entity.Gallery;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GalleryMapper {

    GalleryImageResponse toResponse(Gallery gallery);

    List<GalleryImageResponse> toResponseList(List<Gallery> galleries);
}
