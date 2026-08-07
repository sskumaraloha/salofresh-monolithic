package com.salofresh.dto.bulk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Summary result of a bulk CSV import operation, including per-row errors.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkImportResult {

    private int totalRows;
    private int successCount;
    private int failureCount;
    private List<BulkImportError> errors;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BulkImportError {
        private int rowNumber;
        private String message;
    }
}
