package com.salofresh.service.impl.user;

import com.salofresh.dto.user.AddressRequest;
import com.salofresh.dto.user.AddressResponse;
import com.salofresh.entity.Address;
import com.salofresh.entity.City;
import com.salofresh.entity.User;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.mapper.user.AddressMapper;
import com.salofresh.repository.AddressRepository;
import com.salofresh.repository.CityRepository;
import com.salofresh.repository.UserRepository;
import com.salofresh.service.user.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final CityRepository cityRepository;
    private final AddressMapper addressMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> listAddresses(Long userId) {
        User user = getUserReference(userId);
        return addressRepository.findAllByUserAndDeletedFalse(user).stream()
                .map(addressMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AddressResponse createAddress(Long userId, AddressRequest request) {
        User user = getUserReference(userId);
        City city = getCity(request.getCityId());

        Address address = addressMapper.toEntity(request);
        address.setUser(user);
        address.setCity(city);
        address.setDefault(request.isDefault());

        if (request.isDefault()) {
            unsetExistingDefault(user);
        }

        Address saved = addressRepository.save(address);
        return addressMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request) {
        User user = getUserReference(userId);
        Address address = getOwnedAddress(user, addressId);
        City city = getCity(request.getCityId());

        addressMapper.updateEntityFromRequest(request, address);
        address.setCity(city);
        address.setDefault(request.isDefault());

        if (request.isDefault()) {
            unsetExistingDefault(user, addressId);
        }

        Address saved = addressRepository.save(address);
        return addressMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        User user = getUserReference(userId);
        Address address = getOwnedAddress(user, addressId);
        address.setDeleted(true);
        addressRepository.save(address);
    }

    @Override
    @Transactional
    public AddressResponse setDefault(Long userId, Long addressId) {
        User user = getUserReference(userId);
        Address address = getOwnedAddress(user, addressId);

        unsetExistingDefault(user, addressId);
        address.setDefault(true);

        Address saved = addressRepository.save(address);
        return addressMapper.toResponse(saved);
    }

    private void unsetExistingDefault(User user) {
        addressRepository.findByUserAndIsDefaultTrueAndDeletedFalse(user)
                .ifPresent(existingDefault -> {
                    existingDefault.setDefault(false);
                    addressRepository.save(existingDefault);
                });
    }

    private void unsetExistingDefault(User user, Long excludingAddressId) {
        addressRepository.findByUserAndIsDefaultTrueAndDeletedFalse(user)
                .filter(existingDefault -> !existingDefault.getId().equals(excludingAddressId))
                .ifPresent(existingDefault -> {
                    existingDefault.setDefault(false);
                    addressRepository.save(existingDefault);
                });
    }

    private Address getOwnedAddress(User user, Long addressId) {
        return addressRepository.findByIdAndUserAndDeletedFalse(addressId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));
    }

    private City getCity(Long cityId) {
        return cityRepository.findById(cityId)
                .orElseThrow(() -> new ResourceNotFoundException("City", "id", cityId));
    }

    private User getUserReference(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }
}
