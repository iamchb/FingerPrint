package com.flueky.fingerprint;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 指纹信息类，系统的被隐藏
 * Created by flueky on 2017/12/12.
 * @deprecated 不支持android 9.0 系统
 */

public class Fingerprint {

    private String mName;
    private int mGroupId;
    private int mFingerId;
    private long mDeviceId;

    public Fingerprint(Object fingerprint) {
        if(fingerprint==null)
            return ;
        Class fingerPrintClass = fingerprint.getClass();
        try {
            Method getNameMethod = fingerPrintClass.getMethod("getName");
            Method getFingerIdMethod = fingerPrintClass.getMethod("getFingerId");
            Method getGroupIdMethod = fingerPrintClass.getMethod("getGroupId");
            Method getDeviceIdMethod = fingerPrintClass.getMethod("getDeviceId");

            mName = (String) getNameMethod.invoke(fingerprint);
            mGroupId = (int) getGroupIdMethod.invoke(fingerprint);
            mFingerId = (int) getFingerIdMethod.invoke(fingerprint);
            mDeviceId = (long) getDeviceIdMethod.invoke(fingerprint);

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        JSONObject json = new JSONObject();
        try {
            json.put("name", mName);
            json.put("groupId", mGroupId);
            json.put("fingerd", mFingerId);
            json.put("deviceId", mDeviceId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString();
    }
}
