package online.sarkhan.tgdownloaderbot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

@Configuration
public class AsyncConfig {
    @Bean
    public Executor downloadExecutor(){
        return new ThreadPoolExecutor(
                4, 8, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
                new ThreadFactory() {
                    private int c=0;
                    public Thread newThread(Runnable r){ return new Thread(r, "dl-"+(c++)); }
                }
        );
    }
}