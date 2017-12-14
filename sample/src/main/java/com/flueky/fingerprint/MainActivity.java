package com.flueky.fingerprint;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.blankj.utilcode.util.EncryptUtils;
import com.flueky.framework.android.activity.BaseActivity;
import com.flueky.framework.android.util.ToastTool;

import java.util.List;

public class MainActivity extends BaseActivity {

    private FingerPrintController fingerPrintController;
    private ListView lvFingerprint;
    private ArrayAdapter<Fingerprint> fingerprintAdapter;
    private SharedPreferences sharedPreferences;

    @Override
    protected int getContentLayout() {
        return R.layout.activity_main;
    }

    @Override
    protected void initView() {
        super.initView();
        lvFingerprint = (ListView) findViewById(R.id.activity_main_lv_fingerpirnts);

    }

    @Override
    protected void initData() {
        fingerPrintController = new FingerPrintController(this);
        if (fingerPrintController.isSupportFingerprint()) {
            fingerprintAdapter = new ArrayAdapter<Fingerprint>(this, R.layout.activity_main_list_item, R.id.activity_main_list_item_tv_content);
            lvFingerprint.setAdapter(fingerprintAdapter);
            List<Fingerprint> fingerprints = fingerPrintController.getEnrolledFingerprints();
            sharedPreferences = getSharedPreferences("fingerprint", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            String fingerMd5Data = genFingerprintSignature(fingerprints);
            editor.putString("finger_data", fingerMd5Data);
            editor.commit();
            fingerprintAdapter.addAll(fingerprints);
        } else {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("提示").setMessage("您的设备不支持指纹")
                    .setNegativeButton("知道了", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .create();
            dialog.setCancelable(true);
            dialog.show();
        }

    }

    /**
     * 生成本地指纹签名。采集信息有，指纹名称、指纹id、分组id和设备id
     *
     * @param fingerprints
     * @return
     */
    private String genFingerprintSignature(List<Fingerprint> fingerprints) {
        StringBuffer buffer = new StringBuffer("fingerprint");
        for (int i = 0; fingerprints != null && i < fingerprints.size(); i++) {
            buffer = new StringBuffer(EncryptUtils.encryptMD5ToString(fingerprints.get(i).toString(), buffer.toString()));
        }
        return buffer.toString();
    }

    /**
     * 开始验证指纹
     *
     * @param v
     */
    public void authenticate(View v) {
        if (!fingerPrintController.hasEnrolledFingerprints()) {
            new AlertDialog.Builder(this)
                    .setTitle("提示").setMessage("您的设备未录入指纹")
                    .setNegativeButton("知道了", null)
                    .create().show();
            return;
        }
        final AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle("提示").setMessage("请验证指纹")
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        fingerPrintController.cancelAuth();
                    }
                }).create();
        alertDialog.show();
        fingerPrintController.authenticate(new AuthenticateResultCallback() {
            @Override
            public void onAuthenticationError(String errorMsg) {
                ToastTool.showShort(MainActivity.this, errorMsg);
                alertDialog.dismiss();
            }

            @Override
            public void onAuthenticationSucceeded() {
                ToastTool.showShort(MainActivity.this, "校验成功");
                alertDialog.dismiss();
            }

            @Override
            public void onAuthenticationFailed() {
                ToastTool.showShort(MainActivity.this, "校验失败,再次尝试");
            }
        });
    }


    /**
     * 判断变更
     *
     * @param v
     */
    public void judgeChange(View v) {
        if (!fingerPrintController.hasEnrolledFingerprints()) {
            new AlertDialog.Builder(this)
                    .setTitle("提示").setMessage("您的设备未录入指纹")
                    .setNegativeButton("知道了", null)
                    .create().show();
            return;
        }

        String oldFingerMd5Data = sharedPreferences.getString("finger_data", "");
        List<Fingerprint> fingerprints = fingerPrintController.getEnrolledFingerprints();
        String newFingerMd5Data = genFingerprintSignature(fingerprints);
        if (newFingerMd5Data.equals(oldFingerMd5Data))//包括重命名指纹，都算指纹变更
            ToastTool.showShort(this, "指纹信息没变");
        else {
            fingerprintAdapter.clear();
            fingerprintAdapter.addAll(fingerprints);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("finger_data", newFingerMd5Data);
            editor.commit();
            ToastTool.showShort(this, "指纹信息改变");
        }
    }
}
