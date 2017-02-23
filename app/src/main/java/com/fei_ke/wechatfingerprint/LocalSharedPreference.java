package com.fei_ke.wechatfingerprint;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

/**
 * Created by fei on 2017/2/23.
 */

public class LocalSharedPreference {
    public static final String KEY_XPOSED_WECHAT_PAY_FINGERPRINT = "xposed_wechat_pay_fingerprint";

    public static final String KEY_PASSWORD = "wechat_pay_password";
    public static final String KEY_IV       = "IV";

    private SharedPreferences preferences;

    public LocalSharedPreference(Context context) {
        preferences = context.getSharedPreferences(KEY_XPOSED_WECHAT_PAY_FINGERPRINT, Activity.MODE_PRIVATE);
    }

    public String getData(String keyName) {
        return preferences.getString(keyName, "");
    }

    public boolean storeData(String key, String data) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, data);
        return editor.commit();
    }

    public boolean containsKey(String key) {
        return !TextUtils.isEmpty(getData(key));
    }
}