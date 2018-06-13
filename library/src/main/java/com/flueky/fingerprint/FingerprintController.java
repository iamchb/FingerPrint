package com.flueky.fingerprint;

import android.app.Activity;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;

import com.flueky.framework.android.util.ToastTool;
import com.orhanobut.logger.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 指纹控制器
 * Created by flueky on 2017/12/8.
 */

public class FingerprintController {
    private FingerprintManager fpManager;
    private Activity activity;
    private FingerprintManager.CryptoObject cryptoObject;
    private CancellationSignal cancellationSignal;

    public FingerprintController(Activity activity) {
        this.activity = activity;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {//默认6.0以下不支持指纹。包括厂商定制的5.x系统
            try {
                Class.forName("android.hardware.fingerprint.FingerprintManager");
                //低于6.0版本，有这个类，不正常。在兼容性测试中，可以抛出异常，筛选出设备。
//                throw new RuntimeException("低于6.0系统支持指纹模块");
            } catch (ClassNotFoundException e) {
                // 低于6.0版本，没有这个类正常
                e.printStackTrace();

            }
            return;
        }
        this.fpManager = (FingerprintManager) activity.getSystemService(Context.FINGERPRINT_SERVICE);
    }

    /**
     * 判断是否支持指纹
     *
     * @return
     */
    public boolean isSupportFingerprint() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)//默认6.0以下不支持指纹。包括厂商定制的5.x系统
            return false;
        return fpManager == null ? false : fpManager.isHardwareDetected();
    }

    /**
     * 是否录入指纹
     *
     * @return
     */
    public boolean hasEnrolledFingerprints() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return false;

        if (fpManager == null)
            throw new NotSupportFingerprintException("Your device is not support fingerprint");


        return fpManager.hasEnrolledFingerprints();
    }

    /**
     * 发起指纹认证识别
     *
     * @param callback
     */
    public void authenticate(final AuthenticateResultCallback callback) {
        if (fpManager == null)
            throw new NotSupportFingerprintException("Your device is not support fingerprint");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return;
        cancellationSignal = new CancellationSignal();
        //版本26才支持指纹
        fpManager.authenticate(null, cancellationSignal, 0, new FingerprintManager.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                if (callback != null)
                    callback.onAuthenticationError(errString.toString());
            }

            @Override
            public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                super.onAuthenticationHelp(helpCode, helpString);
                ToastTool.showShort(FingerprintController.this.activity, helpString.toString());
            }

            @Override
            public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                if (callback != null)
                    callback.onAuthenticationSucceeded();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                if (callback != null)
                    callback.onAuthenticationFailed();
            }
        }, null);
    }

    public void cancelAuth() {

        if (fpManager == null)
            throw new NotSupportFingerprintException("Your device is not support fingerprint");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
            return;
        if (cancellationSignal != null && !cancellationSignal.isCanceled())
            cancellationSignal.cancel();
    }

    /**
     * 获取已经录入的指纹
     *
     * @return
     */
    public List<Fingerprint> getEnrolledFingerprints() {
        if (fpManager == null)
            throw new NotSupportFingerprintException("Your device is not support fingerprint");
        List<Fingerprint> fingerprints = new ArrayList<Fingerprint>();
        Class fpManagerClass = fpManager.getClass();
        try {
            Method getEnrolledFingerprints = fpManagerClass.getMethod("getEnrolledFingerprints");
            Object fingerPrints = getEnrolledFingerprints.invoke(fpManager);
            if (fingerPrints != null) {
                ArrayList<Class<?>> fingerPrintList = (ArrayList<Class<?>>) fingerPrints;
                for (int i = 0; fingerPrintList != null && i < fingerPrintList.size(); i++) {
                    Fingerprint fingerprint = new Fingerprint(fingerPrintList.get(i));
                    fingerprints.add(fingerprint);
                    Logger.d("%s", fingerprint.toString());
                }
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return fingerprints;
    }
}
