package com.cafeteriamanagement.dto;

public class FileContentResponseDTO {

    private String path;
    private String content;

    public FileContentResponseDTO(String path, String content) {
        this.path = path;
        this.content = content;
    }

    public String getPath() { return path; }
    public String getContent() { return content; }
}
