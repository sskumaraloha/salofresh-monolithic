package com.salofresh.service.inventory;

import com.salofresh.dto.inventory.ProductRequest;
import com.salofresh.dto.inventory.ProductResponse;

import java.util.List;

public interface ProductService {

    ProductResponse create(Long salonId, ProductRequest request);

    ProductResponse update(Long salonId, Long productId, ProductRequest request);

    void delete(Long salonId, Long productId);

    ProductResponse getById(Long salonId, Long productId);

    List<ProductResponse> listBySalon(Long salonId);

    List<ProductResponse> listLowStock(Long salonId);
}
