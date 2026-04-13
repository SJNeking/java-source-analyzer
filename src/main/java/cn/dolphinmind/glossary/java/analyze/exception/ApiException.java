package cn.dolphinmind.glossary.java.analyze.exception;

/**
 * 业务异常基类
 * 
 * 所有业务相关的异常都应继承此类，携带 HTTP 状态码和业务错误码。
 */
public class ApiException extends RuntimeException {

    private final int statusCode;
    private final String errorCode;

    public ApiException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = String.valueOf(statusCode);
    }

    public ApiException(int statusCode, String errorCode, String message) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public ApiException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorCode = String.valueOf(statusCode);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
