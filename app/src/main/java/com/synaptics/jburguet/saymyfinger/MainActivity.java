package com.synaptics.jburguet.saymyfinger;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebBackForwardList;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

// For fingerprint API
import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.pass.Spass;
import com.samsung.android.sdk.pass.SpassFingerprint;
import com.samsung.android.sdk.pass.SpassInvalidStateException;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // TAG used for debugging info, and webview object
    private static final String TAG = "------------------>";
    private WebView mWebView;
    private EditText mEditText;
    private Spinner mSpinner;
    private String mUrl = "https://en.m.wikipedia.org/wiki/Synaptics";
    List<String> mBookmarks = new ArrayList<String>();

    // Create a timer (see http://stackoverflow.com/questions/4597690)
    private Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            startIdentify();
            timerHandler.postDelayed(this, 500);
        }
    };


    // ====================== Fingerprint related variables=======================================//

    private Context mContext;
    private SpassFingerprint mSpassFingerprint;
    private Spass mSpass;
    private ArrayList<Integer> designatedFingers = null;
    private boolean needRetryIdentify = false;
    private boolean onReadyIdentify = false;
    private boolean isFeatureEnabled_fingerprint = false;
    private boolean isFeatureEnabled_index = false;

    // ====================== Set up Broadcast Receiver ==========================================//

    private BroadcastReceiver mPassReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (SpassFingerprint.ACTION_FINGERPRINT_RESET.equals(action)) {
                Toast.makeText(mContext, "all fingerprints are removed", Toast.LENGTH_SHORT).show();
            } else if (SpassFingerprint.ACTION_FINGERPRINT_REMOVED.equals(action)) {
                int fingerIndex = intent.getIntExtra("fingerIndex", 0);
                Toast.makeText(mContext, fingerIndex + " fingerprints is removed", Toast.LENGTH_SHORT).show();
            } else if (SpassFingerprint.ACTION_FINGERPRINT_ADDED.equals(action)) {
                int fingerIndex = intent.getIntExtra("fingerIndex", 0);
                Toast.makeText(mContext, fingerIndex + " fingerprints is added", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void registerBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(SpassFingerprint.ACTION_FINGERPRINT_RESET);
        filter.addAction(SpassFingerprint.ACTION_FINGERPRINT_REMOVED);
        filter.addAction(SpassFingerprint.ACTION_FINGERPRINT_ADDED);
        mContext.registerReceiver(mPassReceiver, filter);
    }

    private void unregisterBroadcastReceiver() {
        try {
            if (mContext != null) {
                mContext.unregisterReceiver(mPassReceiver);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void resetAll() {
        designatedFingers = null;
        needRetryIdentify = false;
        onReadyIdentify = false;
    }

    // ====================== Set up Spass Fingerprint listener Object ===========================//

    private SpassFingerprint.IdentifyListener mIdentifyListener = new SpassFingerprint.IdentifyListener() {
        @Override
        public void onFinished(int eventStatus)
        {
            int FingerprintIndex = 0;
            String FingerprintGuideText = null;
            try {
                FingerprintIndex = mSpassFingerprint.getIdentifiedFingerprintIndex();
            } catch (IllegalStateException ise) {
            }
            if (eventStatus == SpassFingerprint.STATUS_AUTHENTIFICATION_SUCCESS)
            {
                fingerprintAction(FingerprintIndex);
            }
            else if (eventStatus == SpassFingerprint.STATUS_TIMEOUT_FAILED) {
            }
            else if (eventStatus == SpassFingerprint.STATUS_QUALITY_FAILED)
            {
                needRetryIdentify = true;
                FingerprintGuideText = mSpassFingerprint.getGuideForPoorQuality();
                Toast.makeText(mContext, FingerprintGuideText, Toast.LENGTH_SHORT).show();
            }
            else {
                needRetryIdentify = true;
            }
            if (!needRetryIdentify) {
                resetIdentifyIndex();
            }
        }

        @Override
        public void onReady() {
        }

        @Override
        public void onStarted() {
        }

        @Override
        public void onCompleted() {
            onReadyIdentify = false;
            if (needRetryIdentify)
            {
                needRetryIdentify = false;
            }
        }
    };

    // ============================ Fingerprint functions ======================================= //

    public void startIdentify() {
        if (onReadyIdentify == false) {
            try {
                onReadyIdentify = true;
                if (mSpassFingerprint != null) {
                    setIdentifyIndex();
                    mSpassFingerprint.startIdentify(mIdentifyListener);
                }
            } catch (SpassInvalidStateException ise) {
                onReadyIdentify = false;
                resetIdentifyIndex();
            } catch (IllegalStateException e) {
                onReadyIdentify = false;
                resetIdentifyIndex();
            }
        }
    }

    private void setIdentifyIndex() {
        if (isFeatureEnabled_index) {
            if (mSpassFingerprint != null && designatedFingers != null)
            {
                mSpassFingerprint.setIntendedFingerprintIndex(designatedFingers);
            }
        }
    }

    private void resetIdentifyIndex() {
        designatedFingers = null;
    }

    // Program each registered fingerprint with chosen action
    public void fingerprintAction(int fingerprintIndex) {
        if (fingerprintIndex == 1)
        {
            urlToClipboard();
        }
        else if (fingerprintIndex == 2 || fingerprintIndex == 3)
        {
            mBookmarks.add(mWebView.getUrl().toString());
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_spinner_item, mBookmarks);
            mSpinner.setAdapter(adapter);


            Toast toast = Toast.makeText(this, "Bookmark Added!", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            ViewGroup group = (ViewGroup) toast.getView();
            TextView messageTextView = (TextView) group.getChildAt(0);
            messageTextView.setTextSize(25);
            toast.show();
        }
    }


    // Normal stuff

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSpass = new Spass();
        mContext = this;

        try {
            mSpass.initialize(this);
        } catch (SsdkUnsupportedException e) {
        } catch (UnsupportedOperationException e) {
        }
        isFeatureEnabled_fingerprint = mSpass.isFeatureEnabled(Spass.DEVICE_FINGERPRINT);
        if (isFeatureEnabled_fingerprint) {
            mSpassFingerprint = new SpassFingerprint(this);
        } else {
            return;
        }

        isFeatureEnabled_index = mSpass.isFeatureEnabled(Spass.DEVICE_FINGERPRINT_FINGER_INDEX);

        registerBroadcastReceiver();


        // Get edit text, show initial url and make it load a new one when ENTER is pressed
        mEditText = (EditText) findViewById(R.id.editText);
        mEditText.setText(mUrl);
        mEditText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    mUrl = mEditText.getText().toString();
                    mWebView.loadUrl(mUrl);
                    return true;
                }
                return false;
            }
        });


        mSpinner = (Spinner) findViewById(R.id.spinner);
//        mSpinner.setVisibility(View.INVISIBLE);

        // Get web view, activate javascript, make it update the edit text and load initial url
        mWebView = (WebView) findViewById(R.id.activity_main_webview);
        mWebView.getSettings().setJavaScriptEnabled(true);  // if not most webs suck
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                mEditText.setText(url.toString());
            }
        });  // force links and redirects to open here, plus update the edit text
        mWebView.loadUrl(mUrl);
        mWebView.requestFocus();  // so we remove the on-screen keyboard at the start

        timerHandler.postDelayed(timerRunnable, 0);  // create a timer and start calling its action
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            // Make sure we update the url in the edit text
            WebBackForwardList mWebBackForwardList = mWebView.copyBackForwardList();
            if (mWebBackForwardList.getCurrentIndex() > 0)
                mEditText.setText(mWebBackForwardList.getItemAtIndex(
                        mWebBackForwardList.getCurrentIndex() - 1).getUrl());

            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    public void urlToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("fingerprinted", mWebView.getUrl().toString());
        clipboard.setPrimaryClip(clip);

        Toast toast = Toast.makeText(this, "URL Copied to Clipboard", Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        ViewGroup group = (ViewGroup) toast.getView();
        TextView messageTextView = (TextView) group.getChildAt(0);
        messageTextView.setTextSize(25);
        toast.show();
    }

    @Override
    protected void onPause() {
        unregisterBroadcastReceiver();
        resetAll();
    }

    @Override
    protected void onResume() {
        mSpass = new Spass();
        mContext = this;

        try {
            mSpass.initialize(this);
        } catch (SsdkUnsupportedException e) {
        } catch (UnsupportedOperationException e) {
        }
        isFeatureEnabled_fingerprint = mSpass.isFeatureEnabled(Spass.DEVICE_FINGERPRINT);
        if (isFeatureEnabled_fingerprint) {
            mSpassFingerprint = new SpassFingerprint(this);
        } else {
            return;
        }

        isFeatureEnabled_index = mSpass.isFeatureEnabled(Spass.DEVICE_FINGERPRINT_FINGER_INDEX);

        registerBroadcastReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterBroadcastReceiver();
        resetAll();
    }
}
