package com.cafeteriamanagement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class FileWriteRequestDTO {

    @NotBlank(message = "Path is required")
    @Schema(description = "Relative path of the file to write", example = "menus/daily-menu.txt")
    private String path;

    @NotNull(message = "Content is required")
    @Schema(description = "Text content to write to the file", example = "Today's menu: Grilled Chicken with Rice")
    private String content;

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
