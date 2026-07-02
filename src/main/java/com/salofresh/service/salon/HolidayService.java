package com.salofresh.service.salon;

import com.salofresh.dto.salon.HolidayRequest;
import com.salofresh.dto.salon.HolidayResponse;

import java.util.List;

public interface HolidayService {

    List<HolidayResponse> list(Long salonId);

    HolidayResponse create(Long salonId, HolidayRequest request);

    HolidayResponse update(Long salonId, Long holidayId, HolidayRequest request);

    void delete(Long salonId, Long holidayId);
}
