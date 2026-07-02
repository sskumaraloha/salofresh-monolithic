package com.salofresh.file;

import com.salofresh.config.AppProperties;
import com.salofresh.exception.FileStorageException;
import com.salofresh.util.RandomCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
@RequiredArgsConstructor
public class LocalFileStorageServiceImpl implements FileStorageService {

    private final AppProperties appProperties;
    private final FileValidator fileValidator;

    @Override
    public String store(MultipartFile file, String subDirectory) {
        fileValidator.validate(file);
        try {
            Path targetDir = Paths.get(appProperties.getFile().getUploadDir(), subDirectory).normalize().toAbsolutePath();
            Files.createDirectories(targetDir);

            String extension = FilenameUtils.getExtension(file.getOriginalFilename());
            String fileName = RandomCodeGenerator.generateAlphanumeric(16) + "." + extension;
            Path targetPath = targetDir.resolve(fileName);

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            return "%s/%s/%s".formatted(appProperties.getFile().getBaseUrl(), subDirectory, fileName);
        } catch (IOException ex) {
            throw new FileStorageException("Failed to store uploaded file", ex);
        }
    }

    @Override
    public void delete(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }
        try {
            String relativePath = fileUrl.replace(appProperties.getFile().getBaseUrl(), "");
            Path filePath = Paths.get(appProperties.getFile().getUploadDir(), relativePath).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new FileStorageException("Failed to delete file: " + fileUrl, ex);
        }
    }
}
