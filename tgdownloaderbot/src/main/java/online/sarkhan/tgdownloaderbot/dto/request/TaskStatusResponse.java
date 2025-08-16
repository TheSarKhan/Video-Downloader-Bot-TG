package online.sarkhan.tgdownloaderbot.dto.request;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class TaskStatusResponse {
    private String taskId;
    private String status;
    private String message;
    private String fileName;
    private Long fileSizeBytes;
    private String url; // kaynak
}