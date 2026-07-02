package com.salofresh.controller.salon;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.salon.CityResponse;
import com.salofresh.dto.salon.CountryResponse;
import com.salofresh.dto.salon.StateResponse;
import com.salofresh.response.ApiResponse;
import com.salofresh.service.salon.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(AppConstants.API_BASE_PATH + "/locations")
@Tag(name = "Locations", description = "Public country/state/city lookups used to populate address and salon forms")
@PreAuthorize("permitAll()")
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/countries")
    @Operation(summary = "List all countries")
    public ResponseEntity<ApiResponse<List<CountryResponse>>> listCountries() {
        return ResponseEntity.ok(ApiResponse.success("Countries fetched successfully", locationService.listCountries()));
    }

    @GetMapping("/states")
    @Operation(summary = "List states for a given country")
    public ResponseEntity<ApiResponse<List<StateResponse>>> listStates(@RequestParam Long countryId) {
        return ResponseEntity.ok(ApiResponse.success("States fetched successfully", locationService.listStates(countryId)));
    }

    @GetMapping("/cities")
    @Operation(summary = "List cities for a given state")
    public ResponseEntity<ApiResponse<List<CityResponse>>> listCities(@RequestParam Long stateId) {
        return ResponseEntity.ok(ApiResponse.success("Cities fetched successfully", locationService.listCities(stateId)));
    }
}
