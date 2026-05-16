package com.cafeteriamanagement.dto;

import java.util.List;

public class FileListResponseDTO {

    private String path;
    private List<String> entries;

    public FileListResponseDTO(String path, List<String> entries) {
        this.path = path;
        this.entries = entries;
    }

    public String getPath() { return path; }
    public List<String> getEntries() { return entries; }
}
