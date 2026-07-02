package com.salofresh.service.impl.salon;

import com.salofresh.constant.CacheNames;
import com.salofresh.dto.salon.CityResponse;
import com.salofresh.dto.salon.CountryResponse;
import com.salofresh.dto.salon.StateResponse;
import com.salofresh.repository.CityRepository;
import com.salofresh.repository.CountryRepository;
import com.salofresh.repository.StateRepository;
import com.salofresh.service.salon.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final CountryRepository countryRepository;
    private final StateRepository stateRepository;
    private final CityRepository cityRepository;

    @Override
    @Cacheable(cacheNames = CacheNames.COUNTRIES)
    @Transactional(readOnly = true)
    public List<CountryResponse> listCountries() {
        return countryRepository.findAll().stream()
                .map(country -> CountryResponse.builder()
                        .id(country.getId())
                        .name(country.getName())
                        .isoCode(country.getIsoCode())
                        .dialCode(country.getDialCode())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(cacheNames = CacheNames.STATES, key = "#countryId")
    @Transactional(readOnly = true)
    public List<StateResponse> listStates(Long countryId) {
        return stateRepository.findAllByCountryId(countryId).stream()
                .map(state -> StateResponse.builder()
                        .id(state.getId())
                        .name(state.getName())
                        .countryId(countryId)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(cacheNames = CacheNames.CITIES, key = "#stateId")
    @Transactional(readOnly = true)
    public List<CityResponse> listCities(Long stateId) {
        return cityRepository.findAllByStateId(stateId).stream()
                .map(city -> CityResponse.builder()
                        .id(city.getId())
                        .name(city.getName())
                        .stateId(stateId)
                        .popular(city.isPopular())
                        .build())
                .collect(Collectors.toList());
    }
}
