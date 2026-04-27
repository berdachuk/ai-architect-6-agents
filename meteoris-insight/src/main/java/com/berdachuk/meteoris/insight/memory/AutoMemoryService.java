package com.berdachuk.meteoris.insight.memory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * AutoMemory with a global {@code MEMORY.md} index and typed entry files
 * ({@code user}, {@code feedback}, {@code project}, {@code reference}).
 *
 * <p>Entries survive across sessions (unlike the old per-session file layout).</p>
 */
@Service
public class AutoMemoryService {

    private final Path root;

    public AutoMemoryService(@Value("${meteoris.automemory.root}") String rootDir) {
        this.root = Path.of(rootDir);
    }

    private Path root() throws IOException {
        if (!Files.isDirectory(root)) {
            Files.createDirectories(root);
        }
        return root;
    }

    private Path memoryIndex() throws IOException {
        return root().resolve("MEMORY.md");
    }

    public void appendEntry(String type, String markdownLine) throws IOException {
        if (type == null || type.isBlank() || markdownLine == null || markdownLine.isBlank()) {
            throw new IllegalArgumentException("type and markdownLine required");
        }
        Path file = root().resolve(type.toLowerCase() + ".md");
        String line = markdownLine.endsWith("\n") ? markdownLine : markdownLine + "\n";
        Files.writeString(file, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        refreshIndex(type, markdownLine.trim());
    }

    public String readEntries(String type) throws IOException {
        if (type == null || type.isBlank()) {
            return "";
        }
        Path file = root().resolve(type.toLowerCase() + ".md");
        if (!Files.isRegularFile(file)) {
            return "";
        }
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    public String readIndex() throws IOException {
        Path idx = memoryIndex();
        if (!Files.isRegularFile(idx)) {
            return "# AutoMemory Index\n\n(Empty)\n";
        }
        return Files.readString(idx, StandardCharsets.UTF_8);
    }

    public String readAll() throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(readIndex());
        List<String> types = List.of("user", "feedback", "project", "reference");
        for (String t : types) {
            String content = readEntries(t);
            if (!content.isBlank()) {
                sb.append("\n## ").append(t).append("\n\n").append(content).append("\n");
            }
        }
        return sb.toString();
    }

    private void refreshIndex(String type, String preview) throws IOException {
        Path idx = memoryIndex();
        String ts = Instant.now().toString();
        String line = "- **" + type + "** @ " + ts + " — " + preview + "\n";
        Files.writeString(idx, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    // Legacy per-session API kept for backward compatibility during transition
    public void appendPreference(String sessionId, String markdownLine) throws IOException {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId required");
        }
        Path dir = root().resolve(sanitize(sessionId));
        Path file = dir.resolve("preferences.md");
        String line = markdownLine.endsWith("\n") ? markdownLine : markdownLine + "\n";
        Files.writeString(file, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public String readPreferences(String sessionId) throws IOException {
        if (sessionId == null || sessionId.isBlank()) {
            return "";
        }
        Path file = root().resolve(sanitize(sessionId)).resolve("preferences.md");
        if (!Files.isRegularFile(file)) {
            return "";
        }
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    private static String sanitize(String id) {
        return id.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
