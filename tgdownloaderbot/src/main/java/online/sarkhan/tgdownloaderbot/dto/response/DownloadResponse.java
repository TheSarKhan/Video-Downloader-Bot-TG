package online.sarkhan.tgdownloaderbot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DownloadResponse {
    private String taskId;
    private String status;
    private String videoId; // yeni eklendi
}
