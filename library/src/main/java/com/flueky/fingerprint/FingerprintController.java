package com.flueky.fingerprint;

import android.app.Activity;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.annotation.RequiresApi;
import android.util.Base64;
import android.util.Log;

import com.flueky.framework.android.util.ToastTool;
import com.orhanobut.logger.Logger;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Signature;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * 指纹控制器
 * Created by flueky on 2017/12/8.
 */

public class FingerprintController {
    private FingerprintManager fpManager;
    private Activity activity;
    private FingerprintManager.CryptoObject cryptoObject;
    private CancellationSignal cancellationSignal;

    private Mac mac;
    private Cipher cipher = null;
    private KeyStore keyStore;
    private KeyGenerator generator;
    static final String DEFAULT_KEY_NAME = "default_key";

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
        } else {
            this.fpManager = (FingerprintManager) activity.getSystemService(Context.FINGERPRINT_SERVICE);
            try {
                // 初始化HmacMD5摘要算法的密钥产生器
                generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
                keyStore = KeyStore.getInstance("AndroidKeyStore");

                cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                        + KeyProperties.BLOCK_MODE_CBC + "/"
                        + KeyProperties.ENCRYPTION_PADDING_PKCS7);
//                createKey(DEFAULT_KEY_NAME, true);
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (NoSuchProviderException e) {
                e.printStackTrace();
            } catch (KeyStoreException e) {
                e.printStackTrace();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void createKey(String keyName, boolean invalidatedByBiometricEnrollment) {
        try {
            keyStore.load(null);

            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(keyName,
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setInvalidatedByBiometricEnrollment(invalidatedByBiometricEnrollment);
            }
            generator.init(builder.build());
            generator.generateKey();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException
                | CertificateException | IOException e) {
            throw new RuntimeException(e);
        }
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

        if (!isSupportFingerprint())
            throw new NotSupportFingerprintException("Your device doesn't enroll fingerprint");

        return fpManager.hasEnrolledFingerprints();
    }

    /**
     * 发起指纹认证识别
     *
     * @param cryptoObject 指纹库加密对象
     * @param callback
     */
    public void authenticate(FingerprintManager.CryptoObject cryptoObject, final AuthenticateResultCallback callback) {
        if (fpManager == null)
            throw new NotSupportFingerprintException("Your device is not support fingerprint");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return;
        cancellationSignal = new CancellationSignal();
        //版本26才支持指纹
        fpManager.authenticate(cryptoObject, cancellationSignal, 0, new FingerprintManager.AuthenticationCallback() {
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

            @RequiresApi(api = Build.VERSION_CODES.M)
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

    private static final String TAG = "FingerprintController";

    /**
     * 发起指纹认证
     *
     * @param callback
     */
    public void authenticate(final AuthenticateResultCallback callback) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return;
        authenticate(null, callback);
    }


    /**
     * 判断指纹库，是否发生变化
     *
     * @return
     */
    public boolean hasChanged() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return false;
        if(!isSupportFingerprint()){// 不支持指纹，默认不会发生变化
            return false;
        }
        try {
            keyStore.load(null);
            SecretKey secretKey = (SecretKey) keyStore.getKey(DEFAULT_KEY_NAME, null);
            if (secretKey == null) {
                createKey(DEFAULT_KEY_NAME, true);
                secretKey = (SecretKey) keyStore.getKey(DEFAULT_KEY_NAME, null);
            }
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return false;
        } catch (KeyPermanentlyInvalidatedException e) {
            e.printStackTrace();
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to init Cipher", e);
        }
    }

    /**
     * 指纹库发生变化时调用
     */
    public void resolveChange() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return;
        createKey(DEFAULT_KEY_NAME, true);
    }

    /**
     * 取消认证
     */
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
     * @deprecated android P 以后已经不支持使用反射执行hide注解方法
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
