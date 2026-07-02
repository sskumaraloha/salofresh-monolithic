package com.salofresh.service.salon;

import com.salofresh.dto.salon.CityResponse;
import com.salofresh.dto.salon.CountryResponse;
import com.salofresh.dto.salon.StateResponse;

import java.util.List;

public interface LocationService {

    List<CountryResponse> listCountries();

    List<StateResponse> listStates(Long countryId);

    List<CityResponse> listCities(Long stateId);
}
