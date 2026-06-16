package com.cafeteriamanagement.service;

import com.cafeteriamanagement.security.SecurityAuditLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileSystemServiceTest {

    @TempDir
    Path tempDir;

    private FileSystemService service;

    @BeforeEach
    void setUp() {
        service = new FileSystemService(tempDir.toString(), new SecurityAuditLogger());
    }

    @Test
    void writeAndRead_roundTrip() throws IOException {
        service.writeFile("notes/todo.txt", "hello world");
        assertEquals("hello world", service.readFile("notes/todo.txt"));
    }

    @Test
    void writeFile_exceedingMaxSize_throws() {
        String tooBig = "a".repeat(1024 * 1024 + 1);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.writeFile("big.txt", tooBig));
        assertTrue(ex.getMessage().contains("maximum allowed size"));
    }

    @Test
    void readFile_missing_throwsNoSuchFile() {
        assertThrows(NoSuchFileException.class, () -> service.readFile("ghost.txt"));
    }

    @Test
    void readFile_onDirectory_throws() throws IOException {
        service.createDirectory("adir");
        assertThrows(IllegalArgumentException.class, () -> service.readFile("adir"));
    }

    @Test
    void createDirectory_createsIt() throws IOException {
        service.createDirectory("reports/2026");
        assertTrue(Files.isDirectory(tempDir.resolve("reports/2026")));
    }

    @Test
    void listDirectory_listsEntriesSorted() throws IOException {
        service.writeFile("b.txt", "x");
        service.writeFile("a.txt", "y");
        List<String> entries = service.listDirectory(null);
        assertEquals(List.of("a.txt", "b.txt"), entries);
    }

    @Test
    void listDirectory_blankPath_listsBase() throws IOException {
        service.writeFile("root.txt", "x");
        assertTrue(service.listDirectory("").contains("root.txt"));
    }

    @Test
    void listDirectory_notADirectory_throws() throws IOException {
        service.writeFile("file.txt", "x");
        assertThrows(IllegalArgumentException.class, () -> service.listDirectory("file.txt"));
    }

    @Test
    void delete_file() throws IOException {
        service.writeFile("temp.txt", "x");
        service.delete("temp.txt");
        assertFalse(Files.exists(tempDir.resolve("temp.txt")));
    }

    @Test
    void delete_directoryRecursively() throws IOException {
        service.writeFile("dir/inner.txt", "x");
        service.delete("dir");
        assertFalse(Files.exists(tempDir.resolve("dir")));
    }

    @Test
    void delete_missing_throwsNoSuchFile() {
        assertThrows(NoSuchFileException.class, () -> service.delete("ghost.txt"));
    }

    // ---- security: path confinement ----

    @Test
    void pathTraversal_isRejected() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.writeFile("../escape.txt", "x"));
        assertTrue(ex.getMessage().contains("Invalid path"));
    }

    @Test
    void blankPath_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.createDirectory(" "));
    }
}
