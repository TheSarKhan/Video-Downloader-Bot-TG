package online.sarkhan.tgdownloaderbot.config;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
@Component
public class VideoCleaner {

    private final Path workDir = Paths.get("./work/work").toAbsolutePath();

    @Scheduled(fixedRate = 100_000) // her saniye çalışır
    public void cleanWorkFolder() {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(workDir)) {
            for (Path file : stream) {
                try {
                    if (Files.isRegularFile(file)) {
                        Files.delete(file);
                        System.out.println("Deleted: " + file.getFileName());
                    }
                } catch (IOException e) {
                    System.err.println("Failed to delete " + file.getFileName() + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Cannot access work directory: " + e.getMessage());
        }
    }
}

