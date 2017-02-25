package com.fei_ke.wechatfingerprint;

import android.content.Context;
import android.transition.Slide;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * FingerPrintLayout
 *
 * Created by fei on 2017/2/24.
 */

public class FingerPrintLayout extends FrameLayout implements FingerPrintContract.View {
    private static final String TAG = "FingerPrintLayout";

    private ImageView mImageView;
    private View      mLayoutFingerprint;
    private TextView  mTextViewHint;
    private View      mGestureDetectView;

    private int colorError, colorSuccess, colorNormal;

    private GestureDetector mGestureDetector;

    private FingerPrintContract.Present mPresent;
    private View                        mViewSwitchVisibility;

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
        mGestureDetectView = findViewById(R.id.gesture_detect_view);
        mViewSwitchVisibility = findViewById(R.id.view_header);

        colorNormal = getResources().getColor(R.color.color_fp_normal);
        colorSuccess = getResources().getColor(R.color.color_fp_success);
        colorError = getResources().getColor(R.color.color_fp_error);

        mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (velocityY > 0) {
                    hideFingerPrintLayout();
                } else if (velocityY < 0) {
                    showFingerPrintLayout();
                }
                return true;
            }

        });

        mViewSwitchVisibility.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLayoutFingerprint.getVisibility() != View.VISIBLE) {
                    showFingerPrintLayout();
                } else {
                    hideFingerPrintLayout();
                }
            }
        });


        mGestureDetectView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mGestureDetector.onTouchEvent(event);
                return true;
            }
        });
        reset();
    }

    public void disableSwitchVisibility() {
        mGestureDetectView.setOnTouchListener(null);
        mViewSwitchVisibility.setOnClickListener(null);
    }

    public void hideFingerPrintLayout() {
        TransitionManager.beginDelayedTransition(FingerPrintLayout.this, new Slide());
        mLayoutFingerprint.setVisibility(View.GONE);

        if (mPresent != null) mPresent.stopAuthenticate();
    }

    public void showFingerPrintLayout() {
        TransitionManager.beginDelayedTransition(FingerPrintLayout.this, new Slide());
        mLayoutFingerprint.setVisibility(View.VISIBLE);

        if (mPresent != null) mPresent.startAuthenticate();
    }

    //public void authSuccess() {
    //    mImageView.getDrawable().setTint(colorSuccess);
    //    mTextViewHint.setText(R.string.auth_success);
    //    mTextViewHint.setTextColor(colorSuccess);
    //}
    //
    //public void authFailure(CharSequence hint) {
    //    mImageView.getDrawable().setTint(colorError);
    //    mTextViewHint.setText(hint);
    //    mTextViewHint.setTextColor(colorError);
    //
    //    //reset
    //    postDelayed(new Runnable() {
    //        @Override
    //        public void run() {
    //            reset();
    //        }
    //    }, 2000);
    //}

    private void reset() {
        mImageView.getDrawable().setTint(colorNormal);
        mTextViewHint.setTextColor(colorNormal);
        mTextViewHint.setText(null);
    }

    @Override
    public void showError(CharSequence errorInfo) {
        mImageView.getDrawable().setTint(colorError);
        mTextViewHint.setText(errorInfo);
        mTextViewHint.setTextColor(colorError);

        //reset
        postDelayed(new Runnable() {
            @Override
            public void run() {
                reset();
            }
        }, 2000);
    }

    @Override
    public void showError(int resId) {
        showError(getContext().getString(resId));
    }

    @Override
    public void showSuccess() {
        mImageView.getDrawable().setTint(colorSuccess);
        mTextViewHint.setText(R.string.auth_success);
        mTextViewHint.setTextColor(colorSuccess);
    }

    @Override
    public void setPresent(FingerPrintContract.Present present) {
        mPresent = present;
    }
}
