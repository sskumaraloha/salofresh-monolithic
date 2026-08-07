package com.salofresh.service.bulk;

import com.salofresh.dto.bulk.BulkImportResult;
import org.springframework.web.multipart.MultipartFile;

public interface BulkDataService {

    byte[] exportCategoriesCsv();

    byte[] exportSalonsCsv();

    byte[] exportUsersCsv();

    BulkImportResult importCategories(MultipartFile file);
}
