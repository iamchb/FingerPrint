package com.flueky.fingerprint;

/**
 * 指纹认证回调
 * Created by flueky on 2017/12/12.
 */

public interface AuthenticateResultCallback {
    void onAuthenticationError(String errorMsg);
    void onAuthenticationSucceeded();
    void onAuthenticationFailed();
}
