package com.fei_ke.wechatfingerprint;

/**
 * Created by fei on 2017/2/23.
 */

import android.hardware.fingerprint.FingerprintManager;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.security.KeyStore;

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
            builder.setBlockModes(KeyProperties.BLOCK_MODE_CBC);
            builder.setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7);
            generator.init(builder.build());
            generator.generateKey();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public FingerprintManager.CryptoObject getCryptoObject(int purpose, byte[] IV) {
        try {
            mStore.load(null);
            final SecretKey key = (SecretKey) mStore.getKey(KEY_NAME, null);
            if (key == null) {
                return null;
            }
            final Cipher cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC
                    + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7);
            if (purpose == KeyProperties.PURPOSE_ENCRYPT) {
                cipher.init(purpose, key);
            } else {
                cipher.init(purpose, key, new IvParameterSpec(IV));
            }
            return new FingerprintManager.CryptoObject(cipher);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}