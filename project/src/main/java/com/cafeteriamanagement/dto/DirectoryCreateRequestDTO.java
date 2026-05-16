package com.cafeteriamanagement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public class DirectoryCreateRequestDTO {

    @NotBlank(message = "Path is required")
    @Schema(description = "Relative path of the directory to create", example = "menus/2027-12")
    private String path;

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
}
