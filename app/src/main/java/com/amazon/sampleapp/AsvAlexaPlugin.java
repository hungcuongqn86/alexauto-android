package com.amazon.sampleapp;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.provider.Settings;
import android.util.Log;

import com.amazon.aace.core.Engine;
import com.amazon.sampleapp.impl.NetworkInfoProvider.NetworkInfoProviderHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.UUID;

public class AsvAlexaPlugin {
    private Activity mActivity;
    private Context mContext;

    private static final int sPermissionRequestCode = 0;
    private static final String[] sRequiredPermissions = {Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_EXTERNAL_STORAGE};

    private static final String sDeviceConfigFile = "app_config.json";

    private MediaPlayer mAudioCueStartVoice; // Voice-initiated listening audio cue
    private MediaPlayer mAudioCueStartTouch; // Touch-initiated listening audio cue
    private MediaPlayer mAudioCueEnd; // End of listening audio cue
    private SharedPreferences mPreferences;

    private LVCConfigReceiver mLVCConfigReceiver;

    // Core
    private Engine mEngine;
    private boolean mEngineStarted = false;
    private NetworkInfoProviderHandler mNetworkInfoProvider;

    public Boolean reqPermission(Context context, Activity activity) {
        Boolean res = true;
        mContext = context;
        mActivity = activity;

        // Check if permissions are missing and must be requested
        ArrayList<String> requests = new ArrayList<>();

        for (String permission : sRequiredPermissions) {
            if (ActivityCompat.checkSelfPermission(mContext, permission)
                    == PackageManager.PERMISSION_DENIED) {
                requests.add(permission);
            }
        }

        // Request necessary permissions if not already granted, else start app
        if (requests.size() > 0) {
            res = false;
            ActivityCompat.requestPermissions(mActivity,
                    requests.toArray(new String[requests.size()]), sPermissionRequestCode);
        }
        return res;
    }

    public void create() {
        initLVC();

        int wakesoundId = mContext.getResources().getIdentifier("med_ui_wakesound", "raw", mActivity.getPackageName());
        int wakesounTouchId = mContext.getResources().getIdentifier("med_ui_wakesound_touch", "raw", mActivity.getPackageName());
        int endpointingTouchId = mContext.getResources().getIdentifier("med_ui_endpointing_touch", "raw", mActivity.getPackageName());

        // Initialize sound effects for speech recognition
        mAudioCueStartVoice = MediaPlayer.create(mContext, wakesoundId);
        mAudioCueStartTouch = MediaPlayer.create(mContext, wakesounTouchId);
        mAudioCueEnd = MediaPlayer.create(mContext, endpointingTouchId);

        // Get shared preferences
        int preferenceFileKeyId = mContext.getResources().getIdentifier("preference_file_key", "string", mActivity.getPackageName());
        mPreferences = mContext.getSharedPreferences(mContext.getString(preferenceFileKeyId),
                Context.MODE_PRIVATE);

        // Retrieve device config from config file and update preferences
        String clientId = "", productId = "", productDsn = "";
        JSONObject config = FileUtils.getConfigFromFile(mContext.getAssets(), sDeviceConfigFile, "config");
        if (config != null) {
            try {
                clientId = config.getString("clientId");
                productId = config.getString("productId");
                Log.i("AsvAlexaPlugin.create", "clientId: " + clientId + " - productId: " + productId + " - productDsn: " + productDsn);
            } catch (JSONException e) {
                Log.w("AsvAlexaPlugin.create", "Missing device info in app_config.json");
            }
            try {
                productDsn = config.getString("productDsn");
            } catch (JSONException e) {
                try {
                    // set Android ID as product DSN
                    productDsn = Settings.Secure.getString(mContext.getContentResolver(),
                            Settings.Secure.ANDROID_ID);
                    Log.i("AsvAlexaPlugin.create", "android id for DSN: " + productDsn);
                } catch (Error error) {
                    productDsn = UUID.randomUUID().toString();
                    Log.w("AsvAlexaPlugin.create", "android id not found, generating random DSN: " + productDsn);
                }
            }
        }
        updateDevicePreferences(clientId, productId, productDsn);
    }

    private void updateDevicePreferences(String clientId,
                                         String productId,
                                         String productDsn) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(mContext.getString(R.string.preference_client_id), clientId);
        editor.putString(mContext.getString(R.string.preference_product_id), productId);
        editor.putString(mContext.getString(R.string.preference_product_dsn), productDsn);
        editor.apply();
    }

    public void onDestroy() {
        Log.i("onDestroy", "Engine stopped");

        if (mAudioCueStartVoice != null) {
            mAudioCueStartVoice.release();
            mAudioCueStartVoice = null;
        }
        if (mAudioCueStartTouch != null) {
            mAudioCueStartTouch.release();
            mAudioCueStartTouch = null;
        }
        if (mAudioCueEnd != null) {
            mAudioCueEnd.release();
            mAudioCueEnd = null;
        }

        if (mLVCConfigReceiver != null) {
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mLVCConfigReceiver);
        }

        if (mNetworkInfoProvider != null) {
            mNetworkInfoProvider.unregister();
        }

        if (mEngine != null) {
            mEngine.dispose();
        }
    }

    private void initLVC() {
        // Register broadcast receiver for configuration from the LVCInteractionService
        try {
            mLVCConfigReceiver = new LVCConfigReceiver();
            IntentFilter filter = new IntentFilter(LVCInteractionService.LVC_RECEIVER_INTENT);
            filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            LocalBroadcastManager.getInstance(mContext).registerReceiver(mLVCConfigReceiver, filter);

            // Start LVCInteractionService to communicate with LVC
            mContext.startService(new Intent(mContext, LVCInteractionService.class));
            Log.i("initLVC", "initLVC success!");
        } catch (RuntimeException e) {
            Log.w("initLVC", "initLVC error:" + e.getMessage());
        }
    }

    /**
     * Broadcast receiver to receive configuration from LVC provided through the
     * {@link LVCInteractionService}
     */
    class LVCConfigReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (LVCInteractionService.LVC_RECEIVER_INTENT.equals(intent.getAction())) {
                if (intent.hasExtra(LVCInteractionService.LVC_RECEIVER_FAILURE_REASON)) {
                    // LVCInteractionService was unable to provide config from LVC
                    String reason = intent.getStringExtra(LVCInteractionService.LVC_RECEIVER_FAILURE_REASON);
                    /////onLVCConfigReceived(null);
                    Log.e("LVCConfigReceiver", "Failed to init LVC: " + reason);
                } else if (intent.hasExtra(LVCInteractionService.LVC_RECEIVER_CONFIGURATION)) {
                    // LVCInteractionService received config from LVC
                    String config = intent.getStringExtra(LVCInteractionService.LVC_RECEIVER_CONFIGURATION);
                    /////onLVCConfigReceived(config);
                    Log.i("LVCConfigReceiver", "Received config from LVC, starting engine now");
                }
            }
        }
    }
}
