package online.sarkhan.tgdownloaderbot.model;

import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Data @Builder
public class DownloadTask {
    private String id;
    private String sourceUrl;
    private Instant createdAt;
    private AtomicReference<TaskStatus> status;
    private String message;
    private Path filePath;
    private Long fileSize;
}