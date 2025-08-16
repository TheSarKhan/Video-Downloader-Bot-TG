package online.sarkhan.tgdownloaderbot.dto.store;


import online.sarkhan.tgdownloaderbot.model.DownloadTask;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TaskStore {
    public static final Map<String, DownloadTask> MAP = new ConcurrentHashMap<>();
    public static void put(DownloadTask t){ MAP.put(t.getId(), t); }
    public static DownloadTask get(String id){ return MAP.get(id); }
}