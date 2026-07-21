package com.gcoedu.core.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class JobProgressService {

    private final StringRedisTemplate redisTemplate;
    private static final String PREFIX = "job_progress:";

    public JobProgressService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void initJob(String jobId, int total) {
        String key = PREFIX + jobId;
        redisTemplate.opsForHash().put(key, "total", String.valueOf(total));
        redisTemplate.opsForHash().put(key, "current", "0");
        redisTemplate.opsForHash().put(key, "percentage", "0");
        redisTemplate.expire(key, 24, TimeUnit.HOURS);
    }

    public void incrementProgress(String jobId, int amount) {
        String key = PREFIX + jobId;
        try {
            redisTemplate.opsForHash().increment(key, "current", amount);
            Object currentObj = redisTemplate.opsForHash().get(key, "current");
            Object totalObj = redisTemplate.opsForHash().get(key, "total");
            
            if (currentObj != null && totalObj != null) {
                double current = Double.parseDouble(currentObj.toString());
                double total = Double.parseDouble(totalObj.toString());
                double percentage = Math.min(100.0, (current / total) * 100.0);
                redisTemplate.opsForHash().put(key, "percentage", String.format("%.2f", percentage).replace(",", "."));
            }
        } catch (Exception e) {
            // Ignora erros pontuais no Redis para não falhar a execução do job principal
        }
    }

    public void failJob(String jobId, String errorMessage) {
        String key = PREFIX + jobId;
        redisTemplate.opsForHash().put(key, "status", "FAILED");
        redisTemplate.opsForHash().put(key, "error", errorMessage);
    }
    
    public void finishJob(String jobId) {
        String key = PREFIX + jobId;
        redisTemplate.opsForHash().put(key, "status", "COMPLETED");
        redisTemplate.opsForHash().put(key, "percentage", "100.00");
    }

    /** Retorna o hash completo do job (status, percentage, error). */
    public Map<Object, Object> getJobStatus(String jobId) {
        return redisTemplate.opsForHash().entries(PREFIX + jobId);
    }
}
