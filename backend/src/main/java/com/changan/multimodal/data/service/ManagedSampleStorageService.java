package com.changan.multimodal.data.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ManagedSampleStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "png", "jpg", "jpeg", "bmp", "gif", "webp",
            "mp4", "mov", "avi",
            "wav", "mp3", "flac",
            "txt", "csv", "json", "xml", "yaml", "yml"
    );

    private final Path storageRoot;

    public ManagedSampleStorageService(@Value("${app.sample-storage.root:./data/sample-assets}") String storageRoot) {
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
    }

    public StoredSample store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("不能上传空文件");
        }
        String originalName = sanitizeName(file.getOriginalFilename());
        String extension = extensionOf(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("不支持的文件类型: " + originalName);
        }
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String storageKey = datePath + "/" + UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path target = resolve(storageKey);
        try {
            Files.createDirectories(target.getParent());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = new DigestInputStream(file.getInputStream(), digest)) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return new StoredSample(
                    storageKey,
                    originalName,
                    file.getContentType(),
                    Files.size(target),
                    HexFormat.of().formatHex(digest.digest())
            );
        } catch (Exception ex) {
            try {
                Files.deleteIfExists(target);
            } catch (IOException ignored) {
                // Keep the original storage failure as the reported cause.
            }
            throw new IllegalStateException("保存样本文件失败: " + originalName, ex);
        }
    }

    public StoredSample storeBytes(byte[] content, String originalName, String contentType) {
        String safeName = sanitizeName(originalName);
        String extension = extensionOf(safeName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("不支持的文件类型: " + safeName);
        }
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String storageKey = datePath + "/" + UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path target = resolve(storageKey);
        try {
            Files.createDirectories(target.getParent());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = new DigestInputStream(new ByteArrayInputStream(content), digest)) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return new StoredSample(
                    storageKey,
                    safeName,
                    contentType,
                    Files.size(target),
                    HexFormat.of().formatHex(digest.digest())
            );
        } catch (Exception ex) {
            try {
                Files.deleteIfExists(target);
            } catch (IOException ignored) {
                // Keep the original storage failure as the reported cause.
            }
            throw new IllegalStateException("保存样本文件失败: " + safeName, ex);
        }
    }

    public Path resolve(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("样本存储键为空");
        }
        Path resolved = storageRoot.resolve(storageKey).normalize();
        if (!resolved.startsWith(storageRoot)) {
            throw new IllegalArgumentException("非法样本存储键");
        }
        return resolved;
    }

    public Path requireExisting(String storageKey) {
        Path path = resolve(storageKey);
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("样本文件不存在或已被移除");
        }
        return path;
    }

    public void deleteQuietly(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException ignored) {
            // Database transaction errors should remain the primary failure.
        }
    }

    private String sanitizeName(String originalName) {
        String name = originalName == null ? "unnamed" : Path.of(originalName).getFileName().toString();
        return name.replaceAll("[\\r\\n\\t]", "_");
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public record StoredSample(String storageKey, String originalName, String contentType, long fileSize, String sha256) {
    }
}
