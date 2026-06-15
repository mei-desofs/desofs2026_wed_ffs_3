package com.cafeteriamanagement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class DirectoryCreateRequestDTO {

    @NotBlank(message = "Path is required")
    @Pattern(
        regexp = "^[a-zA-Z0-9_\\-]+(/[a-zA-Z0-9_\\-]+)*$",
        message = "Path must be a relative path with safe characters only (no .. or absolute paths)"
    )
    @Schema(description = "Relative path of the directory to create", example = "menus/2027-12")
    private String path;

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
}
