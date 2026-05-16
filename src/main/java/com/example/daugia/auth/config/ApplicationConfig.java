package com.example.daugia.auth.config;

import com.github.benmanes.caffeine.cache.CaffeineSpec;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
@EnableCaching
@Slf4j
public class ApplicationConfig implements AsyncConfigurer {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean(name = "domainEventExecutor")
    public Executor domainEventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("domain-event-");
        executor.setRejectedExecutionHandler((r, executor1) -> {
            log.error("Task rejected: task={}", r.getClass().getName());
        });
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, obj) -> {
            log.error("Uncaught async exception in task class {}: {}", method.getDeclaringClass().getName(), throwable.getMessage(), throwable);
        };
    }

    @Bean
    public LockProvider lockProvider(RedisConnectionFactory connectionFactory) {
        return new RedisLockProvider(connectionFactory, "daugia");
    }

    @Bean
    public CacheManager cacheManager() {
        CaffeineSpec spec = CaffeineSpec.parse("maximumSize=500,expireAfterWrite=5m");
        return new CaffeineCacheManager() {{
            setCaffeineSpec(spec);
            setCacheNames(List.of("categories", "auctions"));
        }};
    }
}
