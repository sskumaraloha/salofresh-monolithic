package com.salofresh.service.salon;

import com.salofresh.dto.salon.WorkingHoursRequest;
import com.salofresh.dto.salon.WorkingHoursResponse;

import java.util.List;

public interface WorkingHoursService {

    List<WorkingHoursResponse> list(Long salonId);

    List<WorkingHoursResponse> bulkUpsert(Long salonId, List<WorkingHoursRequest> requests);
}
