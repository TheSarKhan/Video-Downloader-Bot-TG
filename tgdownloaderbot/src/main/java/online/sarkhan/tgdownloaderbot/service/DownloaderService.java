package online.sarkhan.tgdownloaderbot.service;

import lombok.RequiredArgsConstructor;
import online.sarkhan.tgdownloaderbot.dto.request.DownloadRequest;
import online.sarkhan.tgdownloaderbot.dto.store.TaskStore;
import online.sarkhan.tgdownloaderbot.model.DownloadTask;
import online.sarkhan.tgdownloaderbot.model.TaskStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Primary
@Service
@RequiredArgsConstructor
public class DownloaderService {

    @Value("${downloader.bin}")
    private String ytdlp;

    @Value("${downloader.workDir}")
    private String workDir;

    @Value("${downloader.outputTemplate}")
    private String outputTemplate;

    @Value("${downloader.timeoutSec}")
    private int timeoutSec;

    @Value("${downloader.maxFileSizeMb}")
    private long maxFileSizeMb;

    @Value("${downloader.cacheTtlHours}")
    private long cacheTtlHours;
private final Executor downloadExecutor;

    public DownloadTask enqueue(DownloadRequest req) {
        String id = UUID.randomUUID().toString();
        Path dir = Paths.get(workDir);
        try {
            Files.createDirectories(dir);
        } catch (IOException ignored) {}

        DownloadTask task = DownloadTask.builder()
                .id(id)
                .sourceUrl(req.getUrl())
                .createdAt(Instant.now())
                .status(new java.util.concurrent.atomic.AtomicReference<>(TaskStatus.QUEUED))
                .build();

        TaskStore.put(task);

        downloadExecutor.execute(() -> runDownload(task, req, dir, id));
        return task;
    }
    public String extractTikTokVideoId(String videoUrl) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    ytdlp,
                    "--no-warnings",
                    "--no-progress",
                    "--no-color",
                    "--print", "id",
                    videoUrl
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String id = reader.readLine();
                if (id == null || id.isBlank()) {
                    throw new RuntimeException("TikTok ID alınamadı");
                }
                return id.trim(); // sadece string olarak döndür
            }
        } catch (IOException e) {
            throw new RuntimeException("TikTok ID alma hatası", e);
        }
    }

    public Path downloadSync(String url) {
        Path dir = Paths.get(workDir);
        try { Files.createDirectories(dir); } catch (IOException ignored) {}

        Path outputPath = dir.resolve(outputTemplate);
        List<String> cmd = new ArrayList<>(List.of(
                ytdlp, "--no-warnings", "--no-progress", "--restrict-filenames",
                "--no-color", "--print", "after_move:filepath",
                "-o", outputPath.toString(),
                "-f", "mp4/best",
                url
        ));

        try {
            Process p = new ProcessBuilder(cmd)
                    .directory(dir.toFile())
                    .redirectErrorStream(true)
                    .start();

            String filePathStr;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                filePathStr = br.readLine();
            }

            boolean finished = p.waitFor(timeoutSec, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                throw new RuntimeException("İndirme zaman aşımı");
            }
            if (filePathStr == null) throw new RuntimeException("Dosya yolu alınamadı");

            Path file = Paths.get(filePathStr);
            if (!Files.exists(file)) throw new RuntimeException("Dosya bulunamadı");

            return file;
        } catch (Exception e) {
            throw new RuntimeException("İndirme hatası: " + e.getMessage());
        }
    }

    private void runDownload(DownloadTask task, DownloadRequest req, Path dir, String taskId) {
        task.getStatus().set(TaskStatus.RUNNING);

        // Cache kontrolü
        if (Boolean.TRUE.equals(req.getUseCache())) {
            Path cacheDir = dir.resolve("_cache");
            try {
                Files.createDirectories(cacheDir);
                String cacheKey = Base64.getUrlEncoder().withoutPadding().encodeToString(req.getUrl().getBytes());
                Path marker = cacheDir.resolve(cacheKey + ".txt");
                if (Files.exists(marker)) {
                    Path cached = Paths.get(Files.readString(marker).trim());
                    if (Files.exists(cached) &&
                            Instant.now().minusSeconds(cacheTtlHours * 3600).isBefore(Files.getLastModifiedTime(cached).toInstant())) {
                        task.setFilePath(cached);
                        task.setFileSize(Files.size(cached));
                        task.setMessage("cache-hit");
                        task.getStatus().set(TaskStatus.SUCCEEDED);
                        return;
                    }
                }
            } catch (Exception ignored) {}
        }

        List<String> cmd = new ArrayList<>();
        Path outputPath = dir.resolve(outputTemplate); // sadece tek sefer path

        cmd.add(ytdlp);
        cmd.add("--no-warnings");
        cmd.add("--no-progress");
        cmd.add("--restrict-filenames");
        cmd.add("--no-color");
        cmd.add("--print");
        cmd.add("after_move:filepath");
        cmd.add("-o");
        cmd.add(outputPath.toString());

        // Format
        if (Boolean.TRUE.equals(req.getAudioOnly())) {
            cmd.addAll(Arrays.asList("-x", "--audio-format", "mp3"));
        } else if (req.getFormat() != null && !req.getFormat().isBlank()) {
            cmd.addAll(Arrays.asList("-f", req.getFormat()));
        } else {
            cmd.addAll(Arrays.asList("-f", "mp4/best"));
        }

        // Proxy
        if (req.getProxy() != null && !req.getProxy().isBlank()) {
            cmd.addAll(Arrays.asList("--proxy", req.getProxy()));
        }

        // Cookies
        if (req.getCookies() != null && !req.getCookies().isBlank()) {
            try {
                Path cfile = Files.createTempFile("cookies-", ".txt");
                Files.writeString(cfile, req.getCookies());
                cmd.addAll(Arrays.asList("--cookies", cfile.toString()));
            } catch (IOException ignored) {}
        }

        cmd.add(req.getUrl());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(dir.toFile());
        pb.redirectErrorStream(true);

        try {
            Process p = pb.start();

            String filePathStr;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                filePathStr = br.readLine();
            }

            boolean finished = p.waitFor(timeoutSec, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                throw new RuntimeException("İndirme zaman aşımı (" + timeoutSec + " sn)");
            }

            if (p.exitValue() != 0) {
                throw new RuntimeException("yt-dlp hata kodu: " + p.exitValue());
            }

            if (filePathStr == null) throw new RuntimeException("Çıktı alınamadı");

            Path file = Paths.get(filePathStr);
            if (!Files.exists(file)) throw new RuntimeException("Dosya bulunamadı: " + file);

            long size = Files.size(file);
            if (size > maxFileSizeMb * 1024 * 1024) {
                Files.deleteIfExists(file);
                throw new RuntimeException("Dosya çok büyük: " + (size / 1024 / 1024) + " MB");
            }

            task.setFilePath(file);
            task.setFileSize(size);
            task.getStatus().set(TaskStatus.SUCCEEDED);

            // Cache yaz
            try {
                Path cacheDir = dir.resolve("_cache");
                Files.createDirectories(cacheDir);
                String cacheKey = Base64.getUrlEncoder().withoutPadding().encodeToString(req.getUrl().getBytes());
                Path marker = cacheDir.resolve(cacheKey + ".txt");
                Files.writeString(marker, file.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (Exception ignored) {}

        } catch (Exception e) {
            task.setMessage(e.getMessage());
            task.getStatus().set(TaskStatus.FAILED);
        }
    }
}
