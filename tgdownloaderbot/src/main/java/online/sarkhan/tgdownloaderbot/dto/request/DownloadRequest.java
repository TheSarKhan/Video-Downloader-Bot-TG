package online.sarkhan.tgdownloaderbot.dto.request;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DownloadRequest {
    @NotBlank
    private String url;

    // Opsiyoneller:
    private String proxy;      // ör: http://user:pass@host:port
    private String cookies;    // ham cookie string (ya da "sessionid=..; csrftoken=..")
    private String format;     // ör: "mp4", "bestvideo+bestaudio/best"
    private Boolean noWatermark; // tiktok için bazen işe yarar: true -> --remux-video mp4
    private Boolean audioOnly; // sadece ses istiyorsan
    private Boolean useCache;  // true ise aynı URL varsa yeniden indirmez
}
