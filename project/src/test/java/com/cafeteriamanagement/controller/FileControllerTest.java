package com.cafeteriamanagement.controller;

import com.cafeteriamanagement.dto.DirectoryCreateRequestDTO;
import com.cafeteriamanagement.dto.FileContentResponseDTO;
import com.cafeteriamanagement.dto.FileListResponseDTO;
import com.cafeteriamanagement.dto.FileWriteRequestDTO;
import com.cafeteriamanagement.service.FileSystemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.file.NoSuchFileException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileControllerTest {

    @Mock
    private FileSystemService fileSystemService;

    @InjectMocks
    private FileController fileController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createDirectory_created() throws Exception {
        DirectoryCreateRequestDTO req = mock(DirectoryCreateRequestDTO.class);
        when(req.getPath()).thenReturn("reports");
        ResponseEntity<?> response = fileController.createDirectory(req);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(fileSystemService).createDirectory("reports");
    }

    @Test
    void writeFile_created() throws Exception {
        FileWriteRequestDTO req = mock(FileWriteRequestDTO.class);
        when(req.getPath()).thenReturn("a.txt");
        when(req.getContent()).thenReturn("hello");
        ResponseEntity<?> response = fileController.writeFile(req);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(fileSystemService).writeFile("a.txt", "hello");
    }

    @Test
    void readFile_found() throws Exception {
        when(fileSystemService.readFile("a.txt")).thenReturn("hello");
        ResponseEntity<FileContentResponseDTO> response = fileController.readFile("a.txt");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("hello", response.getBody().getContent());
    }

    @Test
    void readFile_notFound() throws Exception {
        when(fileSystemService.readFile("ghost.txt")).thenThrow(new NoSuchFileException("ghost.txt"));
        ResponseEntity<FileContentResponseDTO> response = fileController.readFile("ghost.txt");
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void listDirectory_ok() throws Exception {
        when(fileSystemService.listDirectory("menus")).thenReturn(List.of("a.txt", "b.txt"));
        ResponseEntity<FileListResponseDTO> response = fileController.listDirectory("menus");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().getEntries().size());
    }

    @Test
    void delete_success() throws Exception {
        ResponseEntity<Void> response = fileController.delete("a.txt");
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(fileSystemService).delete("a.txt");
    }

    @Test
    void delete_notFound() throws Exception {
        doThrow(new NoSuchFileException("ghost.txt")).when(fileSystemService).delete("ghost.txt");
        ResponseEntity<Void> response = fileController.delete("ghost.txt");
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
