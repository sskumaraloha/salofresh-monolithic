package com.salofresh.service.gdpr;

import com.salofresh.common.enums.DataRequestStatus;
import com.salofresh.dto.gdpr.CreateDataRequestRequest;
import com.salofresh.dto.gdpr.DataSubjectRequestResponse;
import com.salofresh.dto.gdpr.RejectDataRequestRequest;
import com.salofresh.response.PagedResponse;
import org.springframework.data.domain.Pageable;

public interface DataSubjectRequestService {

    DataSubjectRequestResponse create(Long userId, CreateDataRequestRequest request);

    PagedResponse<DataSubjectRequestResponse> listMine(Long userId, Pageable pageable);

    DataSubjectRequestResponse getMine(Long userId, Long requestId);

    PagedResponse<DataSubjectRequestResponse> listAll(DataRequestStatus status, Pageable pageable);

    DataSubjectRequestResponse processExport(Long adminUserId, Long requestId);

    DataSubjectRequestResponse processDeletion(Long adminUserId, Long requestId);

    DataSubjectRequestResponse reject(Long adminUserId, Long requestId, RejectDataRequestRequest request);
}
