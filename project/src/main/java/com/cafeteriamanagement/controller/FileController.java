package com.cafeteriamanagement.controller;

import com.cafeteriamanagement.dto.DirectoryCreateRequestDTO;
import com.cafeteriamanagement.dto.FileContentResponseDTO;
import com.cafeteriamanagement.dto.FileListResponseDTO;
import com.cafeteriamanagement.dto.FileWriteRequestDTO;
import com.cafeteriamanagement.service.FileSystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@Tag(name = "File System", description = "Server-side file and directory operations for cafeteria management")
@SecurityRequirement(name = "bearerAuth")
public class FileController {

    private final FileSystemService fileSystemService;

    @Autowired
    public FileController(FileSystemService fileSystemService) {
        this.fileSystemService = fileSystemService;
    }

    @PostMapping("/directory")
    @Operation(summary = "Create directory", description = "Creates a directory at the given relative path inside the server's file storage area")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Directory created"),
        @ApiResponse(responseCode = "400", description = "Invalid path", content = @Content)
    })
    public ResponseEntity<Map<String, String>> createDirectory(
            @Valid @RequestBody DirectoryCreateRequestDTO request) throws IOException {
        fileSystemService.createDirectory(request.getPath());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Directory created", "path", request.getPath()));
    }

    @PostMapping
    @Operation(summary = "Write file", description = "Writes text content to a file at the given relative path (creates parent directories as needed)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "File written"),
        @ApiResponse(responseCode = "400", description = "Invalid path or content too large", content = @Content)
    })
    public ResponseEntity<Map<String, String>> writeFile(
            @Valid @RequestBody FileWriteRequestDTO request) throws IOException {
        fileSystemService.writeFile(request.getPath(), request.getContent());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "File written", "path", request.getPath()));
    }

    @GetMapping
    @Operation(summary = "Read file", description = "Returns the text content of a file at the given relative path")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File content",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = FileContentResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "File not found", content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid path", content = @Content)
    })
    public ResponseEntity<FileContentResponseDTO> readFile(
            @Parameter(description = "Relative path of the file", example = "menus/daily-menu.txt")
            @RequestParam String path) throws IOException {
        try {
            String content = fileSystemService.readFile(path);
            return ResponseEntity.ok(new FileContentResponseDTO(path, content));
        } catch (NoSuchFileException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/list")
    @Operation(summary = "List directory", description = "Lists files and subdirectories at the given relative path (defaults to root storage directory)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Directory listing",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = FileListResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid path or not a directory", content = @Content)
    })
    public ResponseEntity<FileListResponseDTO> listDirectory(
            @Parameter(description = "Relative path of the directory (empty = root)", example = "menus")
            @RequestParam(required = false, defaultValue = "") String path) throws IOException {
        List<String> entries = fileSystemService.listDirectory(path);
        return ResponseEntity.ok(new FileListResponseDTO(path, entries));
    }

    @DeleteMapping
    @Operation(summary = "Delete file or directory", description = "Deletes a file or directory (recursively) at the given relative path")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Deleted", content = @Content),
        @ApiResponse(responseCode = "404", description = "File or directory not found", content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid path", content = @Content)
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Relative path of the file or directory to delete", example = "menus/old-menu.txt")
            @RequestParam String path) throws IOException {
        try {
            fileSystemService.delete(path);
            return ResponseEntity.noContent().build();
        } catch (NoSuchFileException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
