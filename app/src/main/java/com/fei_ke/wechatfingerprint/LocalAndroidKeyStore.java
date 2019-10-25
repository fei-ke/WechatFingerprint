package com.fei_ke.wechatfingerprint;

/**
 * Created by fei on 2017/2/23.
 */

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class LocalAndroidKeyStore {
    public static final String KEY_NAME = "xposed_wechat_fingerprint_key";

    private KeyStore mStore;

    LocalAndroidKeyStore() {
        try {
            mStore = KeyStore.getInstance("AndroidKeyStore");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void generateKey(String keyAlias) {
        // 这里使用 AES + CBC + PADDING_PKCS7，并且需要用户验证方能取出
        try {
            final KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            mStore.load(null);
            final int purpose = KeyProperties.PURPOSE_DECRYPT | KeyProperties.PURPOSE_ENCRYPT;
            final KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(keyAlias, purpose);
            builder.setUserAuthenticationRequired(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setInvalidatedByBiometricEnrollment(true);
            }
            builder.setBlockModes(KeyProperties.BLOCK_MODE_CBC);
            builder.setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7);
            generator.init(builder.build());
            generator.generateKey();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean initCipher(Cipher cipher, int purpose, byte[] IV)
            throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, IOException,
            KeyStoreException, InvalidAlgorithmParameterException, InvalidKeyException {

        mStore.load(null);
        final SecretKey key = (SecretKey) mStore.getKey(KEY_NAME, null);
        if (purpose == KeyProperties.PURPOSE_ENCRYPT) {
            cipher.init(purpose, key);
        } else {
            cipher.init(purpose, key, new IvParameterSpec(IV));
        }
        return true;
    }
}