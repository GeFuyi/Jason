package com.egg.springboot_egg.config.aspectandeventlistener;

import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomP6SpyFormatter implements MessageFormattingStrategy {

    // 获取日志对象
    private static final Logger logger = LoggerFactory.getLogger(CustomP6SpyFormatter.class);

    // ANSI 亮红色
    private static final String BRIGHT_RED = "\u001B[91m";
    private static final String RESET = "\u001B[0m";

    @Override
    public String formatMessage(int connectionId, String now, long elapsed, String category,
                                String prepared, String sql, String url) {
        if (sql == null || sql.trim().isEmpty()) {
            return "";
        }

        // 拼接日志信息
        String logMsg = String.format("执行SQL耗时 %d ms: %s", elapsed, sql.trim());

        // 亮红色输出
        logger.info(BRIGHT_RED + logMsg + RESET);

        // 返回空，避免 P6Spy 自己再输出一次
        return "";
    }
}
