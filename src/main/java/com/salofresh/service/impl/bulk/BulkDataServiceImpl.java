package com.salofresh.service.impl.bulk;

import com.salofresh.common.enums.ServiceCategoryType;
import com.salofresh.dto.bulk.BulkImportResult;
import com.salofresh.dto.bulk.BulkImportResult.BulkImportError;
import com.salofresh.entity.Category;
import com.salofresh.entity.Salon;
import com.salofresh.entity.User;
import com.salofresh.exception.BadRequestException;
import com.salofresh.repository.CategoryRepository;
import com.salofresh.repository.SalonRepository;
import com.salofresh.repository.UserRepository;
import com.salofresh.service.bulk.BulkDataService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BulkDataServiceImpl implements BulkDataService {

    private final CategoryRepository categoryRepository;
    private final SalonRepository salonRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public byte[] exportCategoriesCsv() {
        List<Category> categories = categoryRepository.findAll();
        return writeCsv(new String[]{"id", "name", "type", "description", "active"}, printer -> {
            for (Category category : categories) {
                printer.printRecord(
                        category.getId(),
                        category.getName(),
                        category.getType() == null ? "" : category.getType().name(),
                        category.getDescription(),
                        category.isActive()
                );
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportSalonsCsv() {
        List<Salon> salons = salonRepository.findAll();
        return writeCsv(new String[]{"id", "name", "ownerEmail", "city", "verificationStatus", "status", "createdAt"}, printer -> {
            for (Salon salon : salons) {
                String ownerEmail = salon.getOwner() != null && salon.getOwner().getUser() != null
                        ? salon.getOwner().getUser().getEmail()
                        : "";
                String cityName = salon.getCity() != null ? salon.getCity().getName() : "";
                printer.printRecord(
                        salon.getId(),
                        salon.getName(),
                        ownerEmail,
                        cityName,
                        salon.getVerificationStatus() == null ? "" : salon.getVerificationStatus().name(),
                        salon.getStatus() == null ? "" : salon.getStatus().name(),
                        salon.getCreatedAt()
                );
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportUsersCsv() {
        List<User> users = userRepository.findAll();
        return writeCsv(new String[]{"id", "fullName", "email", "phone", "roles", "accountStatus", "createdAt"}, printer -> {
            for (User user : users) {
                String roles = user.getRoles() == null ? "" : user.getRoles().stream()
                        .map(role -> role.getName() == null ? "" : role.getName().name())
                        .collect(Collectors.joining(","));
                printer.printRecord(
                        user.getId(),
                        user.getFullName(),
                        user.getEmail(),
                        user.getPhone(),
                        roles,
                        user.getAccountStatus() == null ? "" : user.getAccountStatus().name(),
                        user.getCreatedAt()
                );
            }
        });
    }

    @Override
    @Transactional
    public BulkImportResult importCategories(MultipartFile file) {
        List<BulkImportError> errors = new ArrayList<>();
        int totalRows = 0;
        int successCount = 0;

        try (InputStreamReader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setTrim(true)
                    .build();
            CSVParser parser = format.parse(reader);

            for (CSVRecord record : parser) {
                totalRows++;
                int rowNumber = (int) record.getRecordNumber();
                try {
                    String name = record.isSet("name") ? record.get("name") : null;
                    String typeRaw = record.isSet("type") ? record.get("type") : null;

                    if (name == null || name.isBlank() || typeRaw == null || typeRaw.isBlank()) {
                        errors.add(BulkImportError.builder()
                                .rowNumber(rowNumber)
                                .message("Missing required field(s): name and type are required")
                                .build());
                        continue;
                    }

                    ServiceCategoryType type;
                    try {
                        type = ServiceCategoryType.valueOf(typeRaw.trim().toUpperCase());
                    } catch (IllegalArgumentException ex) {
                        errors.add(BulkImportError.builder()
                                .rowNumber(rowNumber)
                                .message("Invalid category type: '" + typeRaw + "'")
                                .build());
                        continue;
                    }

                    if (categoryRepository.existsByType(type)) {
                        errors.add(BulkImportError.builder()
                                .rowNumber(rowNumber)
                                .message("Category with type '" + type + "' already exists, skipped")
                                .build());
                        continue;
                    }

                    String description = record.isMapped("description") && record.isSet("description")
                            ? record.get("description") : null;
                    String activeRaw = record.isMapped("active") && record.isSet("active")
                            ? record.get("active") : null;
                    boolean active = activeRaw == null || activeRaw.isBlank() || Boolean.parseBoolean(activeRaw.trim());

                    Category category = Category.builder()
                            .name(name.trim())
                            .type(type)
                            .description(description)
                            .active(active)
                            .build();
                    categoryRepository.save(category);
                    successCount++;
                } catch (Exception ex) {
                    errors.add(BulkImportError.builder()
                            .rowNumber(rowNumber)
                            .message("Unexpected error: " + ex.getMessage())
                            .build());
                }
            }
        } catch (IOException ex) {
            throw new BadRequestException("Invalid CSV file");
        }

        return BulkImportResult.builder()
                .totalRows(totalRows)
                .successCount(successCount)
                .failureCount(totalRows - successCount)
                .errors(errors)
                .build();
    }

    private byte[] writeCsv(String[] headers, CsvWriterAction action) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(headers).build())) {
            action.write(printer);
            printer.flush();
            return out.toByteArray();
        } catch (IOException ex) {
            throw new BadRequestException("Failed to generate CSV file");
        }
    }

    @FunctionalInterface
    private interface CsvWriterAction {
        void write(CSVPrinter printer) throws IOException;
    }
}
