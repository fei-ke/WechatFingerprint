package com.fei_ke.wechatfingerprint;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";


    private FingerprintManager mFingerprintManager;

    private CancellationSignal    mCancellationSignal;
    private LocalAndroidKeyStore  mLocalAndroidKeyStore;
    private LocalSharedPreference mLocalSharedPreference;

    private int mPurpose = Cipher.ENCRYPT_MODE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLocalAndroidKeyStore = new LocalAndroidKeyStore();
        mLocalSharedPreference = new LocalSharedPreference(this);

        if (!mLocalSharedPreference.containsKey(LocalSharedPreference.KEY_PASSWORD)) {
            mLocalAndroidKeyStore.generateKey(LocalAndroidKeyStore.KEY_NAME);
        }

        mFingerprintManager = getSystemService(FingerprintManager.class);


        findViewById(R.id.buttonAuth).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String data = mLocalSharedPreference.getData(LocalSharedPreference.KEY_IV);
                Base64.decode(data, Base64.URL_SAFE);

                if (TextUtils.isEmpty(data)) {
                    mPurpose = Cipher.ENCRYPT_MODE;
                } else {
                    mPurpose = Cipher.DECRYPT_MODE;
                }
                startAuthenticate();
            }
        });
        findViewById(R.id.buttonSetPassword).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPurpose = Cipher.ENCRYPT_MODE;
                startAuthenticate();
            }
        });
    }

    private void startAuthenticate() {
        mCancellationSignal = new CancellationSignal();
        if (checkSelfPermission(Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "need permission", Toast.LENGTH_SHORT).show();
            return;
        }

        String data = mLocalSharedPreference.getData(LocalSharedPreference.KEY_IV);
        mFingerprintManager.authenticate(
                mLocalAndroidKeyStore.getCryptoObject(mPurpose, Base64.decode(data, Base64.URL_SAFE)),
                mCancellationSignal,
                0,
                new AuthenticationCallback(),
                null);
    }

    public void stopAuthenticate() {
        if (mCancellationSignal != null) {
            mCancellationSignal.cancel();
            mCancellationSignal = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopAuthenticate();
    }


    class AuthenticationCallback extends FingerprintManager.AuthenticationCallback {
        public AuthenticationCallback() {
            super();
        }

        @Override
        public void onAuthenticationError(int errorCode, CharSequence errString) {
            super.onAuthenticationError(errorCode, errString);
            Log.d(TAG, "onAuthenticationError() called with: errorCode = [" + errorCode + "], errString = [" + errString + "]");
        }

        @Override
        public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
            super.onAuthenticationHelp(helpCode, helpString);
            Log.d(TAG, "onAuthenticationHelp() called with: helpCode = [" + helpCode + "], helpString = [" + helpString + "]");
        }

        @Override
        public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
            super.onAuthenticationSucceeded(result);
            Log.d(TAG, "onAuthenticationSucceeded() called with: result = [" + result + "]");

            final Cipher cipher = result.getCryptoObject().getCipher();

            if (mPurpose == Cipher.ENCRYPT_MODE) {
                try {
                    byte[] encrypted = cipher.doFinal("123456".getBytes());
                    byte[] IV = cipher.getIV();

                    String encryptedText = Base64.encodeToString(encrypted, Base64.URL_SAFE);
                    String IVText = Base64.encodeToString(IV, Base64.URL_SAFE);

                    mLocalSharedPreference.storeData(LocalSharedPreference.KEY_PASSWORD, encryptedText);
                    mLocalSharedPreference.storeData(LocalSharedPreference.KEY_IV, IVText);

                    Log.i(TAG, "encryptedText: " + encryptedText);

                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                } catch (BadPaddingException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    String data = mLocalSharedPreference.getData(LocalSharedPreference.KEY_PASSWORD);
                    byte[] decrypted = cipher.doFinal(Base64.decode(data, Base64.URL_SAFE));
                    Log.i(TAG, "decrypted: " + new String(decrypted));
                } catch (BadPaddingException | IllegalBlockSizeException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onAuthenticationFailed() {
            super.onAuthenticationFailed();
            Log.d(TAG, "onAuthenticationFailed() called");
        }
    }
}
