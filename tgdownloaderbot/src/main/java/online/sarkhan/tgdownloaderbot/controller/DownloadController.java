package online.sarkhan.tgdownloaderbot.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.sarkhan.tgdownloaderbot.dto.request.DownloadRequest;
import online.sarkhan.tgdownloaderbot.dto.request.TaskStatusResponse;
import online.sarkhan.tgdownloaderbot.dto.response.DownloadResponse;
import online.sarkhan.tgdownloaderbot.dto.store.TaskStore;
import online.sarkhan.tgdownloaderbot.model.DownloadTask;
import online.sarkhan.tgdownloaderbot.service.DownloaderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class DownloadController {

    private final DownloaderService service;

    @PostMapping("/download")
    public ResponseEntity<DownloadResponse> download(@Valid @RequestBody DownloadRequest req){
        String videoId = service.extractTikTokVideoId(req.getUrl()); // yeni ekleme
        DownloadTask t = service.enqueue(req);
        return ResponseEntity.ok(new DownloadResponse(
                t.getId(),
                t.getStatus().get().name(),
                videoId
        ));
    }
    @GetMapping("/tasks/{id}")
    public ResponseEntity<TaskStatusResponse> status(@PathVariable String id){
        var t = Optional.ofNullable(TaskStore.get(id));
        if (t.isEmpty()) return ResponseEntity.notFound().build();

        var task = t.get();
        return ResponseEntity.ok(TaskStatusResponse.builder()
                .taskId(task.getId())
                .status(task.getStatus().get().name())
                .message(task.getMessage())
                .fileName(task.getFilePath()!=null ? task.getFilePath().getFileName().toString() : null)
                .fileSizeBytes(task.getFileSize())
                .url(task.getSourceUrl())
                .build());
    }
}
