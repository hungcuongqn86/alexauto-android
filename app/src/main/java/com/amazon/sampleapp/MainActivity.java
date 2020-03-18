/*
 * Copyright 2017-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazon.sampleapp;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.provider.Settings;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.amazon.aace.alexa.AlexaClient;
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

// AlexaComms Imports

// AmazonLite Imports

// LVC Imports

// AutoVoiceChrome Imports

// DCM Imports

public class MainActivity extends AppCompatActivity implements Observer {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String sDeviceConfigFile = "app_config.json";
    private static final int sPermissionRequestCode = 0;
    private static final String[] sRequiredPermissions = {Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_EXTERNAL_STORAGE};

    /* AACE Platform Interface Handlers */

    // Alexa
    private AlertsHandler mAlerts;
    private AlexaClientHandler mAlexaClient;
    private AudioPlayerHandler mAudioPlayer;
    private AuthProviderHandler mAuthProvider;
    private PlaybackControllerHandler mPlaybackController;
    private SpeechRecognizerHandler mSpeechRecognizer;
    private SpeechSynthesizerHandler mSpeechSynthesizer;
    private AlexaSpeakerHandler mAlexaSpeaker;

    // Alexa Comms Handler

    // LVC Handlers

    // Core
    private Engine mEngine;
    private boolean mEngineStarted = false;

    // Audio
    private AudioInputProviderHandler mAudioInputProvider;
    private AudioOutputProviderHandler mAudioOutputProvider;

    // Network
    private NetworkInfoProviderHandler mNetworkInfoProvider;
    /* Shared Preferences */
    private SharedPreferences mPreferences;

    /* Speech Recognition Components */
    private boolean mIsTalkButtonLongPressed = false;

    private MediaPlayer mAudioCueStartVoice; // Voice-initiated listening audio cue
    private MediaPlayer mAudioCueStartTouch; // Touch-initiated listening audio cue
    private MediaPlayer mAudioCueEnd; // End of listening audio cue

    private GlobalPresetHandler mGlobalPresetHandler;

    private MenuItem mTapToTalkIcon;
    // Earcon Settings
    private boolean mDisableStartOfRequestEarcon;
    private boolean mDisableEndOfRequestEarcon;

    // Lock for Earcons
    private Object mDisableStartOfRequestEarconLock = new Object();
    private Object mDisableEndOfRequestEarconLock = new Object();

    private AsvAlexaPlugin asvAlexaPlugin = new AsvAlexaPlugin();
    /* AutoVoiceChrome Controller */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (asvAlexaPlugin.reqPermission(this, this)) {
            create();
        }
    }

    private void create() {
        // Set the main view content
        setContentView(R.layout.activity_main);
        // Add support action toolbar for action buttons
        setSupportActionBar((Toolbar) findViewById(R.id.actionToolbar));
        asvAlexaPlugin.create();
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
            Log.e(TAG, "Could not start engine. Reason: " + e.getMessage());
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
        File cacheDir = getCacheDir();
        File appDataDir = new File(cacheDir, "appdata");

        // Copy certs from assets to certs subdirectory of cache directory
        File certsDir = new File(appDataDir, "certs");
        FileUtils.copyAllAssets(getAssets(), "certs", certsDir, false);

        // Copy models from assets to certs subdirectory of cache directory.
        // Force copy the models on every start so that the models on device cache are always the latest
        // from the APK
        File modelsDir = new File(appDataDir, "models");
        FileUtils.copyAllAssets(getAssets(), "models", modelsDir, true);

        // Create AAC engine
        mEngine = Engine.create(this);
        ArrayList<EngineConfiguration> configuration = getEngineConfigurations(json, appDataDir, certsDir, modelsDir);

        EngineConfiguration[] configurationArray = configuration.toArray(new EngineConfiguration[configuration.size()]);
        boolean configureSucceeded = mEngine.configure(configurationArray);
        if (!configureSucceeded) throw new RuntimeException("Engine configuration failed");

        // Create the platform implementation handlers and register them with the engine

        // AudioInputProvider
        if (!mEngine.registerPlatformInterface(
                mAudioInputProvider = new AudioInputProviderHandler(this)
        )
        ) throw new RuntimeException("Could not register AudioInputProvider platform interface");

        // AudioInputProvider
        if (!mEngine.registerPlatformInterface(
                mAudioOutputProvider = new AudioOutputProviderHandler(this)
        )
        ) throw new RuntimeException("Could not register AudioOutputProvider platform interface");

        // AlexaClient
        if (!mEngine.registerPlatformInterface(
                mAlexaClient = new AlexaClientHandler(this)
        )
        ) throw new RuntimeException("Could not register AlexaClient platform interface");

        // PlaybackController
        if (!mEngine.registerPlatformInterface(
                mPlaybackController = new PlaybackControllerHandler(this)
        )
        ) throw new RuntimeException("Could not register PlaybackController platform interface");

        // SpeechRecognizer
        boolean wakeWordSupported = false;
        if (!mEngine.registerPlatformInterface(
                mSpeechRecognizer = new SpeechRecognizerHandler(this, wakeWordSupported, true)
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
                mAlexaSpeaker = new AlexaSpeakerHandler(this)
        )
        ) throw new RuntimeException("Could not register AlexaSpeaker platform interface");

        // Alerts
        if (!mEngine.registerPlatformInterface(
                mAlerts = new AlertsHandler(this)
        )
        ) throw new RuntimeException("Could not register Alerts platform interface");

        // NetworkInfoProvider
        if (!mEngine.registerPlatformInterface(
                mNetworkInfoProvider = new NetworkInfoProviderHandler(this, mEngine)
        )
        ) throw new RuntimeException("Could not register NetworkInfoProvider platform interface");

        // CBL Auth Handler
        LoginWithAmazonCBL LoginHandler = new LoginWithAmazonCBL(this);

        // AuthProvider
        if (!mEngine.registerPlatformInterface(
                mAuthProvider = new AuthProviderHandler(this, LoginHandler)
        )
        ) throw new RuntimeException("Could not register AuthProvider platform interface");

        // Set auth handler as connection observer
        mNetworkInfoProvider.registerNetworkConnectionObserver(LoginHandler);

        // Mock global preset
        if (!mEngine.registerPlatformInterface(
                mGlobalPresetHandler = new GlobalPresetHandler(this)
        )) throw new RuntimeException("Could not register Mock Global Preset platform interface");

        // Start the engine
        if (!mEngine.start()) throw new RuntimeException("Could not start engine");
        mEngineStarted = true;

        mAuthProvider.onInitialize();
        initTapToTalk();
    }

    /**
     * Get the configurations to start the Engine
     *
     * @param json       JSON string with LVC config if LVC is supported, null otherwise.
     * @param appDataDir path to app's data directory
     * @param certsDir   path to certificates directory
     * @return List of Engine configurations
     */
    private ArrayList<EngineConfiguration> getEngineConfigurations(String json, File appDataDir, File certsDir, File modelsDir) {
        // Configure the engine
        String productDsn = mPreferences.getString(getString(R.string.preference_product_dsn), "");
        String clientId = mPreferences.getString(getString(R.string.preference_client_id), "");
        String productId = mPreferences.getString(getString(R.string.preference_product_id), "");

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

        // AlexaComms Config

        // AmazonLite Config

        // LVC Config

        // DCM Config

        return configuration;
    }

    @Override
    public void onDestroy() {
        asvAlexaPlugin.onDestroy();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_main, menu);
        // Set tap-to-talk and hold-to-talk actions
        mTapToTalkIcon = menu.findItem(R.id.action_talk);
        initTapToTalk();
        return true;
    }

    private void initTapToTalk() {
        if (mTapToTalkIcon != null && mAlexaClient != null && mSpeechRecognizer != null) {
            mTapToTalkIcon.setActionView(R.layout.menu_item_talk);

            // Set hold-to-talk action
            mTapToTalkIcon.getActionView().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mAlexaClient.getConnectionStatus()
                            == AlexaClient.ConnectionStatus.CONNECTED) {
                        mSpeechRecognizer.onTapToTalk();
                    } else {
                        // Notify Error state to AutoVoiceChrome
                        String message = "AlexaClient not connected. ConnectionStatus: "
                                + mAlexaClient.getConnectionStatus();
                        Log.w(TAG, message);
                    }
                }
            });

            // Start hold-to-talk button action
            mTapToTalkIcon.getActionView().setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (mAlexaClient.getConnectionStatus()
                            == AlexaClient.ConnectionStatus.CONNECTED) {
                        mIsTalkButtonLongPressed = true;
                        mSpeechRecognizer.onHoldToTalk();
                    } else {
                        Log.w(TAG, "ConnectionStatus: DISCONNECTED");
                    }
                    return true;
                }
            });

            // Release hold-to-talk button action
            mTapToTalkIcon.getActionView().setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent m) {
                    // Talk button released
                    if (m.getAction() == MotionEvent.ACTION_UP) {
                        if (mIsTalkButtonLongPressed) {
                            mIsTalkButtonLongPressed = false;
                            mSpeechRecognizer.onReleaseHoldToTalk();
                        }
                    }
                    return false;
                }
            });
        }
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
}
