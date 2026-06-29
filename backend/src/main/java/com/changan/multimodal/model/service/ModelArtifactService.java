package com.changan.multimodal.model.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ModelArtifactService {
    private final Path storageRoot;

    public ModelArtifactService(@Value("${app.model-storage.root:./data/model-artifacts}") String storageRoot) {
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
    }

    public Map<String, Object> store(List<MultipartFile> files, List<String> relativePaths, String kind) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("Please select at least one model file.");
        }

        String uploadId = LocalDate.now() + "-" + UUID.randomUUID().toString().substring(0, 8);
        Path uploadRoot = storageRoot.resolve(uploadId).normalize();
        ensureInsideStorage(uploadRoot);

        List<String> savedFiles = new ArrayList<>();
        long totalBytes = 0;
        try {
            Files.createDirectories(uploadRoot);
            for (int index = 0; index < files.size(); index++) {
                MultipartFile file = files.get(index);
                String requestedPath = relativePath(relativePaths, index, file.getOriginalFilename());
                Path target = uploadRoot.resolve(requestedPath).normalize();
                ensureInside(target, uploadRoot);
                Files.createDirectories(target.getParent());
                try (InputStream input = file.getInputStream()) {
                    Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
                }
                totalBytes += file.getSize();
                savedFiles.add(uploadRoot.relativize(target).toString());
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to store model artifact: " + exception.getMessage(), exception);
        }

        boolean directory = "directory".equalsIgnoreCase(kind) || files.size() > 1;
        Path firstSavedPath = Path.of(savedFiles.get(0));
        Path packagePath = directory && firstSavedPath.getNameCount() > 1
                ? uploadRoot.resolve(firstSavedPath.getName(0)).normalize()
                : uploadRoot.resolve(firstSavedPath).normalize();
        return Map.of(
                "packageUri", packagePath.toString(),
                "artifactKind", directory ? "directory" : "file",
                "fileCount", savedFiles.size(),
                "totalBytes", totalBytes,
                "files", savedFiles
        );
    }

    private String relativePath(List<String> relativePaths, int index, String originalName) {
        String candidate = relativePaths != null && index < relativePaths.size()
                ? relativePaths.get(index)
                : originalName;
        if (candidate == null || candidate.isBlank()) {
            candidate = "artifact-" + index;
        }
        return candidate.replace('\\', '/').replaceFirst("^/+", "");
    }

    private void ensureInsideStorage(Path target) {
        ensureInside(target, storageRoot);
    }

    private void ensureInside(Path target, Path root) {
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Invalid artifact path.");
        }
    }
}
