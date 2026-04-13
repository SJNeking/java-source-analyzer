package cn.dolphinmind.glossary.java.analyze.api;

import cn.dolphinmind.glossary.java.analyze.exception.ApiException;

import java.util.logging.Logger;

/**
 * 全局异常处理器
 * 
 * 捕获所有业务异常并转换为统一的 API 响应格式。
 */
public class GlobalExceptionHandler {

    private static final Logger logger = Logger.getLogger(GlobalExceptionHandler.class.getName());

    /**
     * 处理业务异常
     */
    public static ApiResponse<?> handleApiException(ApiException e) {
        logger.warning("API Error [" + e.getErrorCode() + "]: " + e.getMessage());
        return ApiResponse.error(e.getMessage(), e.getErrorCode());
    }

    /**
     * 处理未知异常
     */
    public static ApiResponse<?> handleUnknownException(Exception e) {
        logger.severe("Unexpected Error: " + e.getMessage());
        return ApiResponse.error("Internal Server Error: " + e.getMessage(), "500");
    }

    /**
     * 统一异常处理入口
     */
    public static ApiResponse<?> handle(Exception e) {
        if (e instanceof ApiException) {
            return handleApiException((ApiException) e);
        }
        return handleUnknownException(e);
    }
}
