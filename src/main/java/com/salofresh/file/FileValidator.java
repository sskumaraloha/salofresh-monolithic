package com.salofresh.file;

import com.salofresh.config.AppProperties;
import com.salofresh.exception.FileStorageException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class FileValidator {

    private final AppProperties appProperties;

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("Uploaded file must not be empty");
        }
        if (file.getSize() > appProperties.getFile().getMaxSizeBytes()) {
            throw new FileStorageException("File size exceeds the maximum allowed size of "
                    + (appProperties.getFile().getMaxSizeBytes() / (1024 * 1024)) + "MB");
        }
        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        if (extension == null || !appProperties.getFile().getAllowedExtensions().contains(extension.toLowerCase())) {
            throw new FileStorageException("Unsupported file type. Allowed types: "
                    + appProperties.getFile().getAllowedExtensions());
        }
    }
}
