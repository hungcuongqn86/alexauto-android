package com.amazon.sampleapp;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.provider.Settings;
import android.util.Log;

import com.amazon.aace.alexa.config.AlexaConfiguration;
import com.amazon.aace.core.CoreProperties;
import com.amazon.aace.core.Engine;
import com.amazon.aace.core.config.ConfigurationFile;
import com.amazon.aace.core.config.EngineConfiguration;
import com.amazon.aace.storage.config.StorageConfiguration;
import com.amazon.aace.vehicle.config.VehicleConfiguration;
import com.amazon.sampleapp.impl.Alerts.AlertsHandler;
import com.amazon.sampleapp.impl.AlexaClient.AlexaClientHandler;
import com.amazon.sampleapp.impl.AlexaSpeaker.AlexaSpeakerHandler;
import com.amazon.sampleapp.impl.Audio.AudioInputProviderHandler;
import com.amazon.sampleapp.impl.Audio.AudioOutputProviderHandler;
import com.amazon.sampleapp.impl.AudioPlayer.AudioPlayerHandler;
import com.amazon.sampleapp.impl.AuthProvider.AuthProviderHandler;
import com.amazon.sampleapp.impl.AuthProvider.LoginWithAmazonCBL;
import com.amazon.sampleapp.impl.GlobalPreset.GlobalPresetHandler;
import com.amazon.sampleapp.impl.NetworkInfoProvider.NetworkInfoProviderHandler;
import com.amazon.sampleapp.impl.PlaybackController.PlaybackControllerHandler;
import com.amazon.sampleapp.impl.SpeechRecognizer.SpeechRecognizerHandler;
import com.amazon.sampleapp.impl.SpeechSynthesizer.SpeechSynthesizerHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Observable;
import java.util.Observer;
import java.util.UUID;

public class AsvAlexaPlugin implements Observer {
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
    private SpeechRecognizerHandler mSpeechRecognizer;
    private AudioInputProviderHandler mAudioInputProvider;
    private AudioOutputProviderHandler mAudioOutputProvider;

    private AlertsHandler mAlerts;
    private AlexaClientHandler mAlexaClient;
    private AudioPlayerHandler mAudioPlayer;
    private AuthProviderHandler mAuthProvider;
    private PlaybackControllerHandler mPlaybackController;
    private SpeechSynthesizerHandler mSpeechSynthesizer;
    private AlexaSpeakerHandler mAlexaSpeaker;
    private GlobalPresetHandler mGlobalPresetHandler;

    // Lock for Earcons
    private boolean mDisableStartOfRequestEarcon;
    private boolean mDisableEndOfRequestEarcon;
    private Object mDisableStartOfRequestEarconLock = new Object();
    private Object mDisableEndOfRequestEarconLock = new Object();

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

    @Override
    public void update(Observable observable, Object object) {
        if (observable instanceof SpeechRecognizerHandler.AudioCueObservable) {
            if (object.equals(SpeechRecognizerHandler.AudioCueState.START_TOUCH)) {
                synchronized (mDisableStartOfRequestEarconLock) {
                    if (!mDisableStartOfRequestEarcon) {
                        // Play touch-initiated listening audio cue
                        mAudioCueStartTouch.start();
                    }
                }
            } else if (object.equals(SpeechRecognizerHandler.AudioCueState.START_VOICE)) {
                synchronized (mDisableStartOfRequestEarconLock) {
                    if (!mDisableStartOfRequestEarcon) {
                        // Play voice-initiated listening audio cue
                        mAudioCueStartVoice.start();
                    }
                }
            } else if (object.equals(SpeechRecognizerHandler.AudioCueState.END)) {
                synchronized (mDisableEndOfRequestEarconLock) {
                    if (!mDisableEndOfRequestEarcon) {
                        // Play stop listening audio cue
                        mAudioCueEnd.start();
                    }
                }
            }
        }
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
     * Continue starting the Engine with the config received from LVC Service.
     *
     * @param config json string with LVC config if LVC is supported, null otherwise
     */
    private void onLVCConfigReceived(String config) {
        // Initialize AAC engine and register platform interfaces
        try {
            if (!mEngineStarted) {
                startEngine(config);
            }
        } catch (RuntimeException e) {
            Log.e("onLVCConfigReceived", "Could not start engine. Reason: " + e.getMessage());
            return;
        }
        mSpeechRecognizer.addObserver(this);
    }

    /**
     * Configure the Engine and register platform interface instances
     *
     * @param json JSON string with LVC config if LVC is supported, null otherwise.
     */
    private void startEngine(String json) throws RuntimeException {

        // Create an "appdata" subdirectory in the cache directory for storing application data
        File cacheDir = mActivity.getCacheDir();
        File appDataDir = new File(cacheDir, "appdata");

        // Copy certs from assets to certs subdirectory of cache directory
        File certsDir = new File(appDataDir, "certs");
        FileUtils.copyAllAssets(mActivity.getAssets(), "certs", certsDir, false);

        // Copy models from assets to certs subdirectory of cache directory.
        // Force copy the models on every start so that the models on device cache are always the latest
        // from the APK
        File modelsDir = new File(appDataDir, "models");
        FileUtils.copyAllAssets(mActivity.getAssets(), "models", modelsDir, true);

        // Create AAC engine
        mEngine = Engine.create(mContext);
        ArrayList<EngineConfiguration> configuration = getEngineConfigurations(json, appDataDir, certsDir, modelsDir);

        EngineConfiguration[] configurationArray = configuration.toArray(new EngineConfiguration[configuration.size()]);
        boolean configureSucceeded = mEngine.configure(configurationArray);
        if (!configureSucceeded) throw new RuntimeException("Engine configuration failed");

        // Create the platform implementation handlers and register them with the engine

        // AudioInputProvider
        if (!mEngine.registerPlatformInterface(
                mAudioInputProvider = new AudioInputProviderHandler(mActivity)
        )
        ) throw new RuntimeException("Could not register AudioInputProvider platform interface");

        // AudioInputProvider
        if (!mEngine.registerPlatformInterface(
                mAudioOutputProvider = new AudioOutputProviderHandler(mActivity)
        )
        ) throw new RuntimeException("Could not register AudioOutputProvider platform interface");

        // AlexaClient
        if (!mEngine.registerPlatformInterface(
                mAlexaClient = new AlexaClientHandler(mActivity)
        )
        ) throw new RuntimeException("Could not register AlexaClient platform interface");

        // PlaybackController
        if (!mEngine.registerPlatformInterface(
                mPlaybackController = new PlaybackControllerHandler(mActivity)
        )
        ) throw new RuntimeException("Could not register PlaybackController platform interface");

        // SpeechRecognizer
        boolean wakeWordSupported = false;
        if (!mEngine.registerPlatformInterface(
                mSpeechRecognizer = new SpeechRecognizerHandler(mActivity, wakeWordSupported, true)
        )
        ) throw new RuntimeException("Could not register SpeechRecognizer platform interface");

        // AudioPlayer
        if (!mEngine.registerPlatformInterface(
                mAudioPlayer = new AudioPlayerHandler(mAudioOutputProvider, mPlaybackController)
        )
        ) throw new RuntimeException("Could not register AudioPlayer platform interface");

        // SpeechSynthesizer
        if (!mEngine.registerPlatformInterface(
                mSpeechSynthesizer = new SpeechSynthesizerHandler()
        )
        ) throw new RuntimeException("Could not register SpeechSynthesizer platform interface");

        // AlexaSpeaker
        if (!mEngine.registerPlatformInterface(
                mAlexaSpeaker = new AlexaSpeakerHandler(mActivity)
        )
        ) throw new RuntimeException("Could not register AlexaSpeaker platform interface");

        // Alerts
        if (!mEngine.registerPlatformInterface(
                mAlerts = new AlertsHandler(mActivity)
        )
        ) throw new RuntimeException("Could not register Alerts platform interface");

        // NetworkInfoProvider
        if (!mEngine.registerPlatformInterface(
                mNetworkInfoProvider = new NetworkInfoProviderHandler(mActivity, mEngine)
        )
        ) throw new RuntimeException("Could not register NetworkInfoProvider platform interface");

        // CBL Auth Handler
        LoginWithAmazonCBL LoginHandler = new LoginWithAmazonCBL(mActivity);

        // AuthProvider
        if (!mEngine.registerPlatformInterface(
                mAuthProvider = new AuthProviderHandler(mActivity, LoginHandler)
        )
        ) throw new RuntimeException("Could not register AuthProvider platform interface");

        // Set auth handler as connection observer
        mNetworkInfoProvider.registerNetworkConnectionObserver(LoginHandler);

        // Mock global preset
        if (!mEngine.registerPlatformInterface(
                mGlobalPresetHandler = new GlobalPresetHandler(mActivity)
        )) throw new RuntimeException("Could not register Mock Global Preset platform interface");

        // Start the engine
        if (!mEngine.start()) throw new RuntimeException("Could not start engine");
        mEngineStarted = true;

        mAuthProvider.onInitialize();

        // initTapToTalk();
    }

    private ArrayList<EngineConfiguration> getEngineConfigurations(String json, File appDataDir, File certsDir, File modelsDir) {
        // Configure the engine
        String productDsn = mPreferences.getString(mContext.getString(R.string.preference_product_dsn), "");
        String clientId = mPreferences.getString(mContext.getString(R.string.preference_client_id), "");
        String productId = mPreferences.getString(mContext.getString(R.string.preference_product_id), "");

        AlexaConfiguration.TemplateRuntimeTimeout[] timeoutList = new AlexaConfiguration.TemplateRuntimeTimeout[]{
                new AlexaConfiguration.TemplateRuntimeTimeout(AlexaConfiguration.TemplateRuntimeTimeoutType.DISPLAY_CARD_TTS_FINISHED_TIMEOUT, 8000),
                new AlexaConfiguration.TemplateRuntimeTimeout(AlexaConfiguration.TemplateRuntimeTimeoutType.DISPLAY_CARD_AUDIO_PLAYBACK_FINISHED_TIMEOUT, 8000),
                new AlexaConfiguration.TemplateRuntimeTimeout(AlexaConfiguration.TemplateRuntimeTimeoutType.DISPLAY_CARD_AUDIO_PLAYBACK_STOPPED_PAUSED_TIMEOUT, 1800000)
        };

        JSONObject config = null;
        ArrayList<EngineConfiguration> configuration = new ArrayList<EngineConfiguration>(Arrays.asList(
                //AlexaConfiguration.createCurlConfig( certsDir.getPath(), "wlan0" ), Uncomment this line to specify the interface name to use by AVS.
                AlexaConfiguration.createCurlConfig(certsDir.getPath()),
                AlexaConfiguration.createDeviceInfoConfig(productDsn, clientId, productId),
                AlexaConfiguration.createMiscStorageConfig(appDataDir.getPath() + "/miscStorage.sqlite"),
                AlexaConfiguration.createCertifiedSenderConfig(appDataDir.getPath() + "/certifiedSender.sqlite"),
                AlexaConfiguration.createAlertsConfig(appDataDir.getPath() + "/alerts.sqlite"),
                AlexaConfiguration.createSettingsConfig(appDataDir.getPath() + "/settings.sqlite"),
                AlexaConfiguration.createNotificationsConfig(appDataDir.getPath() + "/notifications.sqlite"),
                // Uncomment the below line to specify the template runtime values
                StorageConfiguration.createLocalStorageConfig(appDataDir.getPath() + "/localStorage.sqlite"),

                // Example Vehicle Config
                VehicleConfiguration.createVehicleInfoConfig(new VehicleConfiguration.VehicleProperty[]{
                        new VehicleConfiguration.VehicleProperty(VehicleConfiguration.VehiclePropertyType.MAKE, "Amazon"),
                        new VehicleConfiguration.VehicleProperty(VehicleConfiguration.VehiclePropertyType.MODEL, "AmazonCarOne"),
                        new VehicleConfiguration.VehicleProperty(VehicleConfiguration.VehiclePropertyType.TRIM, "Advance"),
                        new VehicleConfiguration.VehicleProperty(VehicleConfiguration.VehiclePropertyType.YEAR, "2025"),
                        new VehicleConfiguration.VehicleProperty(VehicleConfiguration.VehiclePropertyType.GEOGRAPHY, "US"),
                        new VehicleConfiguration.VehicleProperty(VehicleConfiguration.VehiclePropertyType.VERSION, String.format(
                                "Vehicle Software Version 1.0 (Auto SDK Version %s)", mEngine.getProperty(CoreProperties.VERSION))),
                        new VehicleConfiguration.VehicleProperty(VehicleConfiguration.VehiclePropertyType.OPERATING_SYSTEM, "Android 8.1 Oreo API Level 26"),
                        new VehicleConfiguration.VehicleProperty(VehicleConfiguration.VehiclePropertyType.HARDWARE_ARCH, "Armv8a"),
                        new VehicleConfiguration.VehicleProperty(VehicleConfiguration.VehiclePropertyType.LANGUAGE, "en-US"),
                        new VehicleConfiguration.VehicleProperty(VehicleConfiguration.VehiclePropertyType.MICROPHONE, "Single, roof mounted"),
                        // If this list is left blank, it will be fetched by the engine using amazon default endpoint
                        new VehicleConfiguration.VehicleProperty(VehicleConfiguration.VehiclePropertyType.COUNTRY_LIST, "US,GB,IE,CA,DE,AT,IN,JP,AU,NZ,FR"),
                        new VehicleConfiguration.VehicleProperty(VehicleConfiguration.VehiclePropertyType.VEHICLE_IDENTIFIER, "123456789a")
                })
        ));

        String endpointConfigPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/aace.json";
        if (new File(endpointConfigPath).exists()) {
            EngineConfiguration alexaEndpointsConfig = ConfigurationFile.create(Environment.getExternalStorageDirectory().getAbsolutePath() + "/aace.json");
            configuration.add(alexaEndpointsConfig);
            Log.i("getEngineConfigurations", "Overriding endpoints");
        }

        return configuration;
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
                    onLVCConfigReceived(null);
                    Log.e("LVCConfigReceiver", "Failed to init LVC: " + reason);
                } else if (intent.hasExtra(LVCInteractionService.LVC_RECEIVER_CONFIGURATION)) {
                    // LVCInteractionService received config from LVC
                    String config = intent.getStringExtra(LVCInteractionService.LVC_RECEIVER_CONFIGURATION);
                    onLVCConfigReceived(config);
                    Log.i("LVCConfigReceiver", "Received config from LVC, starting engine now");
                }
            }
        }
    }
}
