package com.fei_ke.wechatfingerprint;

import javax.crypto.Cipher;

/**
 * Created by fei on 17/2/25.
 */

public interface FingerPrintContract {
    interface View {
        void showError(CharSequence errorInfo);

        void showError(int resId);

        void showSuccess();

        void setPresent(Present present);

        void reset();
    }

    interface Present {
        void startAuthenticate();

        void stopAuthenticate();

        void encrypt(Cipher cipher, String data);

        String decrypt(Cipher cipher);

    }
}
