package com.salofresh.service.user;

import com.salofresh.dto.user.AddressRequest;
import com.salofresh.dto.user.AddressResponse;

import java.util.List;

public interface AddressService {

    List<AddressResponse> listAddresses(Long userId);

    AddressResponse createAddress(Long userId, AddressRequest request);

    AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request);

    void deleteAddress(Long userId, Long addressId);

    AddressResponse setDefault(Long userId, Long addressId);
}
