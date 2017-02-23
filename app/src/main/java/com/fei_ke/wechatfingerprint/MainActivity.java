package com.fei_ke.wechatfingerprint;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import javax.crypto.Cipher;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";


    private FingerPrintHelper mFingerPrintHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFingerPrintHelper = new FingerPrintHelper(this);

        findViewById(R.id.buttonAuth).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFingerPrintHelper.setPurpose(FingerPrintHelper.DECRYPT_MODE);
                mFingerPrintHelper.startAuthenticate();
            }
        });
        findViewById(R.id.buttonSetPassword).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFingerPrintHelper.setPurpose(FingerPrintHelper.ENCRYPT_MODE);
                mFingerPrintHelper.startAuthenticate();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mFingerPrintHelper.stopAuthenticate();
    }
}
