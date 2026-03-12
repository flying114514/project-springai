package com.ai.common.async;

import com.ai.common.constant.AsyncTaskStreamConstants;
import com.ai.infrastructure.redis.RedisService;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Redis Stream 生产者模板基类。
 * 统一消息发送骨架与失败处理逻辑
 * 我们需要设计三个继承类,分别用来实现分析,评估,向量化的任务
 */

@Slf4j
public abstract class AbstractStreamProducer<T> {

    private final RedisService redisService;

    protected AbstractStreamProducer(RedisService redisService) {
        this.redisService = redisService;
    }

    /**
     * 下面这些参数是由子类提供的
     */
    // Stream 的键名
    protected abstract String streamKey();

    // 消息内容
    protected abstract Map<String, String> buildMessage(T payload);

    // 任务显示名称
    protected abstract String taskDisplayName();

    // 有效载荷标识符
    protected abstract String payloadIdentifier(T payload);

    // 发送失败
    protected abstract void onSendFailed(T payload, String error);

    protected void sendTask(T payload) {
        try {
            String messageId = redisService.streamAdd(
                    streamKey(),
                    buildMessage(payload),
                    AsyncTaskStreamConstants.STREAM_MAX_LEN // Stream 最大长度限制
            );
            log.info("{}任务已发送到Stream: {}, messageId={}",
                    taskDisplayName(), payloadIdentifier(payload), messageId);
        } catch (Exception e) {
            log.error("发送{}任务失败: {}, error={}",
                    taskDisplayName(), payloadIdentifier(payload), e.getMessage(), e);
            // 用来处理错误的处理逻辑
            onSendFailed(payload, "任务入队失败: " + e.getMessage());
        }
    }

    /**
     * 截断错误信息,防止错误信息长度太长压垮数据库
     * @param error
     * @return error.length() > 500 ? error.substring(0, 500) : error;
     */
    protected String truncateError(String error) {
        if (error == null) {
            return null;
        }
        return error.length() > 500 ? error.substring(0, 500) : error;
    }
}
