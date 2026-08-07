package com.salofresh.service.impl.inventory;

import com.salofresh.dto.inventory.ProductRequest;
import com.salofresh.dto.inventory.ProductResponse;
import com.salofresh.entity.Product;
import com.salofresh.entity.Salon;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.mapper.inventory.ProductMapper;
import com.salofresh.repository.ProductRepository;
import com.salofresh.service.inventory.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final InventoryAccessGuard inventoryAccessGuard;

    @Override
    @Transactional
    public ProductResponse create(Long salonId, ProductRequest request) {
        Salon salon = inventoryAccessGuard.requireOwnedSalon(salonId);
        Product product = productMapper.toEntity(request);
        product.setSalon(salon);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse update(Long salonId, Long productId, ProductRequest request) {
        inventoryAccessGuard.requireOwnedSalon(salonId);
        Product product = findProduct(salonId, productId);
        productMapper.updateEntityFromRequest(request, product);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public void delete(Long salonId, Long productId) {
        inventoryAccessGuard.requireOwnedSalon(salonId);
        Product product = findProduct(salonId, productId);
        product.setDeleted(true);
        productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(Long salonId, Long productId) {
        inventoryAccessGuard.requireOwnedSalon(salonId);
        return productMapper.toResponse(findProduct(salonId, productId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> listBySalon(Long salonId) {
        inventoryAccessGuard.requireOwnedSalon(salonId);
        return productMapper.toResponseList(productRepository.findAllBySalonIdAndDeletedFalse(salonId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> listLowStock(Long salonId) {
        inventoryAccessGuard.requireOwnedSalon(salonId);
        List<Product> lowStock = productRepository.findAllBySalonIdAndDeletedFalse(salonId).stream()
                .filter(Product::isBelowReorderLevel)
                .toList();
        return productMapper.toResponseList(lowStock);
    }

    private Product findProduct(Long salonId, Long productId) {
        return productRepository.findByIdAndSalonIdAndDeletedFalse(productId, salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
    }
}
