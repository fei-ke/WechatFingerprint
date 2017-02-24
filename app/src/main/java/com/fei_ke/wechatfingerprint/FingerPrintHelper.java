package com.fei_ke.wechatfingerprint;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.CancellationSignal;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

/**
 * Created by fei on 17/2/24.
 */

public class FingerPrintHelper extends FingerprintManager.AuthenticationCallback {
    private static final String TAG = "FingerPrintHelper";

    public static final int ENCRYPT_MODE = Cipher.ENCRYPT_MODE;
    public static final int DECRYPT_MODE = Cipher.DECRYPT_MODE;

    private FingerprintManager    mFingerprintManager;
    private CancellationSignal    mCancellationSignal;
    private LocalAndroidKeyStore  mLocalAndroidKeyStore;
    private LocalSharedPreference mLocalSharedPreference;

    private int mPurpose = ENCRYPT_MODE;

    private Callback mCallback;

    private Context mContext;

    public FingerPrintHelper(Context context) {
        super();

        mContext = context;

        mLocalAndroidKeyStore = new LocalAndroidKeyStore();
        mLocalSharedPreference = new LocalSharedPreference(context);

        if (!mLocalSharedPreference.containsKey(LocalSharedPreference.KEY_PASSWORD)) {
            mLocalAndroidKeyStore.generateKey(LocalAndroidKeyStore.KEY_NAME);
        }

        mFingerprintManager = context.getSystemService(FingerprintManager.class);
    }

    public void setPurpose(int purpose) {
        mPurpose = purpose;
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public void startAuthenticate() {
        String IV = mLocalSharedPreference.getData(LocalSharedPreference.KEY_IV);

        if (TextUtils.isEmpty(IV)) {
            mPurpose = ENCRYPT_MODE;
        }

        mCancellationSignal = new CancellationSignal();
        if (mContext.checkSelfPermission(Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(mContext, "need permission", Toast.LENGTH_SHORT).show();
            return;
        }


        mFingerprintManager.authenticate(
                mLocalAndroidKeyStore.getCryptoObject(mPurpose, Base64.decode(IV, Base64.URL_SAFE)),
                mCancellationSignal,
                0,
                this,
                null);
    }


    public void stopAuthenticate() {
        if (mCancellationSignal != null) {
            mCancellationSignal.cancel();
            mCancellationSignal = null;
        }
    }

    @Override
    public void onAuthenticationError(int errorCode, CharSequence errString) {
        super.onAuthenticationError(errorCode, errString);

        if (mCallback != null) mCallback.onFailure(errString);

        Log.d(TAG, "onAuthenticationError() called with: errorCode = [" + errorCode + "], errString = [" + errString + "]");
    }

    @Override
    public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
        super.onAuthenticationHelp(helpCode, helpString);

        if (mCallback != null) mCallback.onFailure(helpString);

        Log.d(TAG, "onAuthenticationHelp() called with: helpCode = [" + helpCode + "], helpString = [" + helpString + "]");
    }

    @Override
    public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
        super.onAuthenticationSucceeded(result);
        Log.d(TAG, "onAuthenticationSucceeded() called with: result = [" + result + "]");

        final Cipher cipher = result.getCryptoObject().getCipher();

        if (mPurpose == ENCRYPT_MODE) {
            try {
                byte[] encrypted = cipher.doFinal("123456".getBytes());
                byte[] IV = cipher.getIV();

                String encryptedText = Base64.encodeToString(encrypted, Base64.URL_SAFE);
                String IVText = Base64.encodeToString(IV, Base64.URL_SAFE);

                mLocalSharedPreference.storeData(LocalSharedPreference.KEY_PASSWORD, encryptedText);
                mLocalSharedPreference.storeData(LocalSharedPreference.KEY_IV, IVText);

                Log.i(TAG, "encryptedText: " + encryptedText);

                if (mCallback != null) mCallback.onSuccess(encryptedText);

            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
            } catch (BadPaddingException e) {
                e.printStackTrace();
            }
        } else {
            try {
                String data = mLocalSharedPreference.getData(LocalSharedPreference.KEY_PASSWORD);
                byte[] decrypted = cipher.doFinal(Base64.decode(data, Base64.URL_SAFE));

                if (mCallback != null) mCallback.onSuccess(new String(decrypted));

            } catch (BadPaddingException | IllegalBlockSizeException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onAuthenticationFailed() {
        super.onAuthenticationFailed();

        if (mCallback != null) mCallback.onFailure("No Match");

        Log.d(TAG, "onAuthenticationFailed() called");
    }

    public interface Callback {
        void onSuccess(String value);

        void onFailure(CharSequence helpString);
    }
}
