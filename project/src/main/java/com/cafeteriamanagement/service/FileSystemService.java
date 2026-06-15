package com.cafeteriamanagement.service;

import com.cafeteriamanagement.security.SecurityAuditLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FileSystemService {

    private static final long MAX_FILE_SIZE_BYTES = 1024 * 1024; // 1 MB

    private final Path basePath;
    private final SecurityAuditLogger auditLogger;

    public FileSystemService(@Value("${app.filesystem.base-dir}") String baseDir,
                             @Autowired SecurityAuditLogger auditLogger) {
        this.auditLogger = auditLogger;
        try {
            this.basePath = Paths.get(baseDir).toAbsolutePath().normalize();
            Files.createDirectories(this.basePath);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot initialize file system base directory: " + baseDir, e);
        }
    }

    public void createDirectory(String relativePath) throws IOException {
        Path target = resolveSafe(relativePath);
        Files.createDirectories(target);
        auditLogger.logFileOperation("CREATE_DIRECTORY", currentUsername(), relativePath);
    }

    public void writeFile(String relativePath, String content) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File content exceeds the maximum allowed size of 1 MB");
        }
        Path target = resolveSafe(relativePath);
        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }
        Files.writeString(target, content, StandardCharsets.UTF_8);
        auditLogger.logFileOperation("WRITE_FILE", currentUsername(), relativePath);
    }

    public String readFile(String relativePath) throws IOException {
        Path target = resolveSafe(relativePath);
        if (!Files.exists(target)) {
            throw new NoSuchFileException(relativePath);
        }
        if (!Files.isRegularFile(target)) {
            throw new IllegalArgumentException("Path does not point to a regular file");
        }
        String content = Files.readString(target, StandardCharsets.UTF_8);
        auditLogger.logFileOperation("READ_FILE", currentUsername(), relativePath);
        return content;
    }

    public List<String> listDirectory(String relativePath) throws IOException {
        Path target = (relativePath == null || relativePath.isBlank())
                ? basePath
                : resolveSafe(relativePath);
        if (!Files.isDirectory(target)) {
            throw new IllegalArgumentException("Path is not a directory or does not exist");
        }
        try (var stream = Files.list(target)) {
            return stream
                    .map(p -> basePath.relativize(p).toString().replace("\\", "/"))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    public void delete(String relativePath) throws IOException {
        Path target = resolveSafe(relativePath);
        if (!Files.exists(target)) {
            throw new NoSuchFileException(relativePath);
        }
        if (Files.isDirectory(target)) {
            try (var walk = Files.walk(target)) {
                walk.sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });
            }
        } else {
            Files.delete(target);
        }
        auditLogger.logFileOperation("DELETE", currentUsername(), relativePath);
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : "unknown";
    }

    // Resolves and validates that the resulting path stays within basePath.
    private Path resolveSafe(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("Path must not be blank");
        }
        Path resolved = basePath.resolve(relativePath).normalize();
        if (!resolved.startsWith(basePath)) {
            // Return a generic message to avoid leaking directory structure
            throw new IllegalArgumentException("Invalid path: access outside the allowed directory is not permitted");
        }
        return resolved;
    }
}
