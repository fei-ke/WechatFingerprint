package com.fei_ke.wechatfingerprint;

import android.content.Context;
import android.transition.Slide;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * FingerPrintLayout
 *
 * Created by fei on 2017/2/24.
 */

public class FingerPrintLayout extends FrameLayout {

    private ImageView mImageView;
    private View      mLayoutFingerprint;
    private TextView  mTextViewHint;

    private int colorError, colorSuccess, colorNormal;

    public FingerPrintLayout(Context context) {
        super(context);
        init(context);
    }

    public FingerPrintLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FingerPrintLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public FingerPrintLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }


    private void init(Context context) {
        View.inflate(context, R.layout.layout_fingerprint, this);

        mLayoutFingerprint = findViewById(R.id.layout_fingerprint);
        mTextViewHint = (TextView) findViewById(R.id.tv_hint);
        mImageView = (ImageView) findViewById(R.id.img_icon);

        colorNormal = getResources().getColor(R.color.color_fp_normal);
        colorSuccess = getResources().getColor(R.color.color_fp_success);
        colorError = getResources().getColor(R.color.color_fp_error);

        findViewById(R.id.view_header).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TransitionManager.beginDelayedTransition(FingerPrintLayout.this, new Slide());
                if (mLayoutFingerprint.getVisibility() != View.VISIBLE) {
                    mLayoutFingerprint.setVisibility(View.VISIBLE);
                } else {
                    mLayoutFingerprint.setVisibility(View.GONE);
                }
            }
        });

        reset();
    }

    public void authSuccess() {
        mImageView.getDrawable().setTint(colorSuccess);
        mTextViewHint.setText(R.string.auth_success);
        mTextViewHint.setTextColor(colorSuccess);
    }

    public void authFailure(CharSequence hint) {
        mImageView.getDrawable().setTint(colorError);
        mTextViewHint.setText(hint);
        mTextViewHint.setTextColor(colorError);

        //reset
        postDelayed(new Runnable() {
            @Override
            public void run() {
                reset();
            }
        }, 2000);
    }

    private void reset() {
        mImageView.getDrawable().setTint(colorNormal);
        mTextViewHint.setTextColor(colorNormal);
        mTextViewHint.setText(null);
    }

}
