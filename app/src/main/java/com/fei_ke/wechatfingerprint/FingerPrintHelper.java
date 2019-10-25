package com.fei_ke.wechatfingerprint;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.CancellationSignal;
import android.security.keystore.KeyProperties;
import android.text.TextUtils;
import android.util.Base64;

import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Created by fei on 17/2/24.
 */

public class FingerPrintHelper extends FingerprintManager.AuthenticationCallback implements FingerPrintContract.Present {
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

    private FingerPrintContract.View mFingerPrintView;

    public FingerPrintHelper(Context context, FingerPrintContract.View fingerPrintView) {
        super();

        this.mContext = context;
        this.mFingerPrintView = fingerPrintView;
        fingerPrintView.setPresent(this);

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

    public synchronized void startAuthenticate() {
        if (mCancellationSignal != null) {
            return;
        }
        String IV = mLocalSharedPreference.getData(LocalSharedPreference.KEY_IV);

        if (TextUtils.isEmpty(IV)) {
            mPurpose = ENCRYPT_MODE;
        }

        if (mContext.checkSelfPermission(Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
            mFingerPrintView.showError(R.string.need_fingerprint_permission);
            return;
        }

        if (!mFingerprintManager.isHardwareDetected()) {
            mFingerPrintView.showError(R.string.hardware_not_supported);
            return;
        }

        if (!mFingerprintManager.hasEnrolledFingerprints()) {
            mFingerPrintView.showError(R.string.register_fingerprint_first);
            return;
        }

        Cipher cipher = null;

        try {
            cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC
                    + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            //ensure not happen
            e.printStackTrace();
        }

        boolean initCipher;
        try {
            initCipher = mLocalAndroidKeyStore.initCipher(cipher, mPurpose, Base64.decode(IV, Base64.URL_SAFE));
        } catch (Exception e) {
            initCipher = false;
        }

        if (!initCipher) {
            mFingerPrintView.showError(R.string.need_set_password);
            if (mPurpose == ENCRYPT_MODE) {
                mLocalAndroidKeyStore.generateKey(LocalAndroidKeyStore.KEY_NAME);
                startAuthenticate();
            }
            return;
        }

        mCancellationSignal = new CancellationSignal();
        mFingerprintManager.authenticate(
                new FingerprintManager.CryptoObject(cipher),
                mCancellationSignal,
                0,
                this,
                null);
    }


    public synchronized void stopAuthenticate() {
        if (mCancellationSignal != null) {
            mCancellationSignal.cancel();
            mCancellationSignal = null;
        }
    }

    @Override
    public void onAuthenticationError(int errorCode, CharSequence errString) {
        if (errorCode == FingerprintManager.FINGERPRINT_ERROR_CANCELED) {
            return;
        }
        mFingerPrintView.showError(errString);

        if (mCallback != null) {
            mCallback.onFailure(errString);
        }
    }

    @Override
    public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
        mFingerPrintView.showError(helpString);

        if (mCallback != null) {
            mCallback.onFailure(helpString);
        }
    }

    @Override
    public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
        mFingerPrintView.showSuccess();

        if (mCallback != null) {
            mCallback.onSuccess(mPurpose, result.getCryptoObject().getCipher());
        }
    }

    @Override
    public void onAuthenticationFailed() {
        mFingerPrintView.showError(R.string.fp_no_match);
        if (mCallback != null) {

            mCallback.onFailure("No Match");
        }
    }

    public void encrypt(Cipher cipher, String data) {
        try {
            byte[] encrypted = cipher.doFinal(data.getBytes());
            byte[] IV = cipher.getIV();

            String encryptedText = Base64.encodeToString(encrypted, Base64.URL_SAFE);
            String IVText = Base64.encodeToString(IV, Base64.URL_SAFE);

            mLocalSharedPreference.storeData(LocalSharedPreference.KEY_PASSWORD, encryptedText);
            mLocalSharedPreference.storeData(LocalSharedPreference.KEY_IV, IVText);

        } catch (IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }
    }

    public String decrypt(Cipher cipher) {
        try {
            String data = mLocalSharedPreference.getData(LocalSharedPreference.KEY_PASSWORD);

            byte[] decrypted = cipher.doFinal(Base64.decode(data, Base64.URL_SAFE));

            return new String(decrypted);

        } catch (BadPaddingException | IllegalBlockSizeException e) {
            return null;
        }
    }

    public interface Callback {
        void onSuccess(int purpose, Cipher cipher);

        void onFailure(CharSequence helpString);
    }
}
