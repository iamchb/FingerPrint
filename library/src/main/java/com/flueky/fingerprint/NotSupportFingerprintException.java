package com.flueky.fingerprint;

/**
 * 不支持指纹异常
 * Created by flueky on 2017/12/12.
 */

public class NotSupportFingerprintException extends RuntimeException {

    public NotSupportFingerprintException() {
        super();
    }

    public NotSupportFingerprintException(String message) {
        super(message);
    }

    public NotSupportFingerprintException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotSupportFingerprintException(Throwable cause) {
        super(cause);
    }

    protected NotSupportFingerprintException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
