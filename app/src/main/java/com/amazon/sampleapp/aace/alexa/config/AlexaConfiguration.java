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

package com.amazon.sampleapp.aace.alexa.config;

import com.amazon.sampleapp.aace.core.config.EngineConfiguration;
import com.amazon.sampleapp.aace.alexa.EqualizerController.EqualizerBand;
import com.amazon.sampleapp.aace.alexa.EqualizerController.EqualizerBandLevel;

/**
 * A factory interface for creating Alexa configuration objects
 */
public class AlexaConfiguration {

    private static final String TAG = "AlexaConfiguration";

    /**
     * Factory method used to programmatically generate device info configuration data.
     * The data generated by this method is equivalent to providing the following JSON
     * values in a configuration file:
     *
     * @code    {.json}
     * {
     *   "deviceInfo":
     *   {
     *     "deviceSerialNumber": "<DEVICE_SERIAL_NUMBER>"
     *     "clientId": "<CLIENT_ID>",
     *     "productId": "<PRODUCT_ID>"
     *   }
     * }
     * @endcode
     *
     * @param  deviceSerialNumber The device serial number used to authorize the client with AVS
     *
     * @param  clientId The client ID used to authorize the client with AVS
     *
     * @param  productId The product ID used to authorize the client with AVS
     */
    public static EngineConfiguration createDeviceInfoConfig( final String deviceSerialNumber, final String clientId, final String productId )
    {
        return new EngineConfiguration() {
            @Override
            protected long createNativeRef() {
                return createDeviceInfoConfigBinder( deviceSerialNumber, clientId, productId );
            }
        };
    }

    // Native Engine JNI methods
    static private native long createDeviceInfoConfigBinder( String deviceSerialNumber, String clientId, String productId );


    /**
     * Factory method used to programmatically generate alerts configuration data.
     * The data generated by this method is equivalent to providing the following JSON
     * values in a configuration file:
     *
     * @code    {.json}
     * {
     *   "alertsCapabilityAgent":
     *   {
     *     "databaseFilePath": "<SQLITE_DATABASE_FILE_PATH>"
     *   }
     * }
     * @endcode
     *
     * @param  databaseFilePath The file path to the SQLite database used to store persistent alerts data.
     * The database will be created on initialization if it does not already exist.
     */
    public static EngineConfiguration createAlertsConfig( final String databaseFilePath )
    {
        return new EngineConfiguration() {
            @Override
            protected long createNativeRef() {
                return createAlertsConfigBinder( databaseFilePath );
            }
        };
    }

    // Native Engine JNI methods
    static private native long createAlertsConfigBinder( String databaseFilePath );

    /**
     * Factory method used to programmatically generate notifications configuration data.
     * The data generated by this method is equivalent to providing the following JSON
     * values in a configuration file:
     *
     * @code    {.json}
     * {
     *   "notifications":
     *   {
     *     "databaseFilePath": "<SQLITE_DATABASE_FILE_PATH>"
     *   }
     * }
     * @endcode
     *
     * @param  databaseFilePath The file path to the SQLite database used to store persistent notifications data.
     * The database will be created on initialization if it does not already exist.
     */
    public static EngineConfiguration createNotificationsConfig( final String databaseFilePath )
    {
        return new EngineConfiguration() {
            @Override
            protected long createNativeRef() {
                return createNotificationsConfigBinder( databaseFilePath );
            }
        };
    }

    // Native Engine JNI methods
    static private native long createNotificationsConfigBinder( String databaseFilePath );

    /**
     * Factory method used to programmatically generate certified sender configuration data.
     * The data generated by this method is equivalent to providing the following JSON
     * values in a configuration file:
     *
     * @code    {.json}
     * {
     *   "certifiedSender":
     *   {
     *     "databaseFilePath": "<SQLITE_DATABASE_FILE_PATH>"
     *   }
     * }
     * @endcode
     *
     * @param  databaseFilePath The file path to the SQLite database used to store persistent certified sender data.
     * The database will be created on initialization if it does not already exist.
     */
    public static EngineConfiguration createCertifiedSenderConfig( final String databaseFilePath )
    {
        return new EngineConfiguration() {
            @Override
            protected long createNativeRef() {
                return createCertifiedSenderConfigBinder( databaseFilePath );
            }
        };
    }

    // Native Engine JNI methods
    static private native long createCertifiedSenderConfigBinder( String databaseFilePath );

    /**
     * Factory method used to programmatically generate CURL configuration data.
     * The 'libCurlUtils' sub-component of the global configuration supports the following options:
     * - CURLOPT_CAPATH If present, specifies a value for the libcurl property CURLOPT_CAPATH.
     * The data generated by this method is equivalent to providing the following JSON
     * values in a configuration file:
     *
     * @code    {.json}
     * {
     *     "libcurlUtils" : {
     *         "CURLOPT_CAPATH" : "<CA_CERTIFICATES_FILE_PATH>"
     *     }
     * }
     * @endcode
     *
     * @param  certsPath The file path to the directory holding CA certificates
     */
    public static EngineConfiguration createCurlConfig( String certsPath ) {
        return createCurlConfig( certsPath, null );
    }

    /**
     * Factory method used to programmatically generate CURL configuration data.
     * The 'libCurlUtils' sub-component of the global configuration supports the following options:
     * - CURLOPT_CAPATH If present, specifies a value for the libcurl property CURLOPT_CAPATH.
     * - CURLOPT_INTERFACE if present, specifies a value for the libcurl property CURLOPT_INTERFACE.
     * The data generated by this method is equivalent to providing the following JSON
     * values in a configuration file:
     *
     * @code    {.json}
     * {
     *     "libcurlUtils" : {
     *         "CURLOPT_CAPATH" : "<CA_CERTIFICATES_FILE_PATH>"
     *         "CURLOPT_INTERFACE" : "<NETWORK_INTERFACE_NAME>"
     *     }
     * }
     * @endcode
     *
     * @param  certsPath The file path to the directory holding CA certificates
     * @param  iface The interface used for outgoing network interface.
     * This can be an network interface name, an IP address or a host name.
     */
    public static EngineConfiguration createCurlConfig( final String certsPath, final String iface  )
    {
        return new EngineConfiguration() {
            @Override
            protected long createNativeRef() {
                return createCurlConfigBinder( certsPath, iface );
            }
        };
    }

    // Native Engine JNI methods
    static private native long createCurlConfigBinder( String certsPath, String iface );

    /**
     * Factory method used to programmatically generate settings configuration data.
     * The data generated by this method is equivalent to providing the following JSON
     * values in a configuration file:
     *
     * @code    {.json}
     * {
     *   "settings": {
     *     "databaseFilePath": "<SQLITE_DATABASE_FILE_PATH>",
     *     "defaultAVSClientSettings": {
     *        "locale": "<LOCALE>"
     *     }
     *   }
     * }
     * @endcode
     *
     * @param  databaseFilePath The file path to the SQLite database used to store persistent settings data.
     * The database will be created on initialization if it does not already exist.
     *
     * @param  locale The current locale setting on the client
     */
    public static EngineConfiguration createSettingsConfig( final String databaseFilePath, final String locale )
    {
        return new EngineConfiguration() {
            @Override
            protected long createNativeRef() {
                return createSettingsConfigBinder( databaseFilePath, locale );
            }
        };
    }

    // Native Engine JNI methods
    static private native long createSettingsConfigBinder( String databaseFilePath, String locale );

    /**
     * Factory method used to programmatically generate settings configuration data.
     * The data generated by this method is equivalent to providing the following JSON
     * values in a configuration file:
     *
     * @code    {.json}
     * {
     *   "settings": {
     *     "databaseFilePath": "<SQLITE_DATABASE_FILE_PATH>",
     *     "defaultAVSClientSettings": {
     *        "locale": "<LOCALE>"
     *     }
     *   }
     * }
     * @endcode
     *
     * @param  databaseFilePath The file path to the SQLite database used to store persistent settings data.
     * The database will be created on initialization if it does not already exist.
     *
     * @param  locale The current locale setting on the client
     */
    public static EngineConfiguration createSettingsConfig( String databaseFilePath ) {
        return AlexaConfiguration.createSettingsConfig( databaseFilePath, "en-US" );
    }

    /**
     * Factory method used to programmatically generate misc storage configuration data.
     * The data generated by this method is equivalent to providing the following JSON
     * values in a configuration file:
     *
     * @code    {.json}
     * {
     *   "miscDatabase":
     *   {
     *     "databaseFilePath": "<SQLITE_DATABASE_FILE_PATH>",
     *   }
     * }
     * @endcode
     *
     * @param  databaseFilePath The file path to the SQLite database used to store persistent misc storage data.
     * The database will be created on initialization if it does not already exist.
     */
    public static EngineConfiguration createMiscStorageConfig( final String databaseFilePath )
    {
        return new EngineConfiguration() {
            @Override
            protected long createNativeRef() {
                return createMiscStorageConfigBinder( databaseFilePath );
            }
        };
    }

    // Native Engine JNI methods
    static private native long createMiscStorageConfigBinder( String databaseFilePath );

    /**
     * Factory method used to programmatically generate system configuration data.
     * The data generated by this method is equivalent to providing the following JSON
     * values in a configuration file:
     *
     * @code    {.json}
     * {
     *   "aace.alexa": {
     *      "system": {
     *          "firmwareVersion": "<FIRMWARE_VERSION>"
     *      }
     *   }
     * }
     * @endcode
     *
     * @param  firmwareVersion The firmware version of the client device
     */
    public static EngineConfiguration createSystemConfig( final int firmwareVersion )
    {
        return new EngineConfiguration() {
            @Override
            protected long createNativeRef() {
                return createSystemConfigBinder( firmwareVersion );
            }
        };
    }

    // Native Engine JNI methods
    static private native long createSystemConfigBinder( int firmwareVersion );

    /**
     * Factory method used to programmatically generate encoder configuration data.
     * The data generated by this method is equivalent to providing the following JSON
     * values in a configuration file:
     *
     * @code{.json}
     * {
     *   "aace.alexa": {
     *      "speechRecognizer": {
     *          "encoder": {
     *               "name": "<ENCODER_NAME>"
     *          }
     *      }
     *   }
     * }
     * @endcode
     *
     * @param encoderName The encoder codec name to be used
     */
    public static EngineConfiguration createSpeechRecognizerConfig( final String encoderName )
    {
        return new EngineConfiguration() {
            @Override
            protected long createNativeRef() {
                return createSpeechRecognizerConfigBinder( encoderName );
            }
        };
    }

    // Native Engine JNI methods
    static private native long createSpeechRecognizerConfigBinder( String encoderName );

    public enum TemplateRuntimeTimeoutType {
        /**
         *  Display card timeout in milli seconds when Alexa completes TTS.
         *  @hideinitializer
         */
        DISPLAY_CARD_TTS_FINISHED_TIMEOUT ( "DISPLAY_CARD_TTS_FINISHED_TIMEOUT","displayCardTTSFinishedTimeout" ),

        /**
         *  Display card timeout in milli seconds when AudioPlayback Completes.
         *  @hideinitializer
         */
        DISPLAY_CARD_AUDIO_PLAYBACK_FINISHED_TIMEOUT ( "DISPLAY_CARD_AUDIO_PLAYBACK_FINISHED_TIMEOUT","displayCardAudioPlaybackFinishedTimeout" ),

        /**
         *  Display card timeout in milli seconds when AudioPlayback is Stopped or Paused.
         *  @hideinitializer
         */
        DISPLAY_CARD_AUDIO_PLAYBACK_STOPPED_PAUSED_TIMEOUT ( "DISPLAY_CARD_AUDIO_PLAYBACK_STOPPED_PAUSED_TIMEOUT","displayCardAudioPlaybackStoppedPausedTimeout" );

        /**
         * @internal
         */
        private String mName;

        /**
         * @internal
         */
        private String mKey;

        /**
         * Type used to identify a TemplateRuntime configuration type and value pair
         */
        TemplateRuntimeTimeoutType( String name, String key ) {
            mName = name;
            mKey = key;
        }

        /**
         * @internal
         */
        public String toString() {
            return mName;
        }

        /**
         * @internal
         */
        public String getKey() {
            return mKey;
        }

    }

    public static class TemplateRuntimeTimeout {
        private TemplateRuntimeTimeoutType mType;
        private int mValue;

        public TemplateRuntimeTimeout( TemplateRuntimeTimeoutType type, int value ) {
            mType = type;
            mValue = value;
        }

        public TemplateRuntimeTimeoutType getType() { return mType; }
        public int getValue() { return mValue; }
    }

    /**
     * Factory method used to programmatically generate template runtime configuration data.
     * This is an optional configuration. Following are the accepted keys and their description.
     * - displayCardTTSFinishedTimeout If present, specifies the values in milli seconds to control the timeout of display card at the Alexa Speech.
     * - displayCardAudioPlaybackFinishedTimeout If present, specifies the values in milli seconds to control the timeout of display card at the FINISHED state of AudioPlayback.
     * - displayCardAudioPlaybackStoppedPausedTimeout If present, specifies the values in milli seconds to control the timeout of display card at STOP or PAUSE state of AudioPlayback.
     * The data generated by this method is equivalent to providing the following JSON
     * values in a configuration file:
     *
     * @code{.json}
     * {
     *   "templateRuntimeCapabilityAgent": {
     *      "displayCardTTSFinishedTimeout": <TIMEOUT_IN_MS>,
     *      "displayCardAudioPlaybackFinishedTimeout": <TIMEOUT_IN_MS>,
     *      "displayCardAudioPlaybackStoppedPausedTimeout": <TIMEOUT_IN_MS>
     *   }
     * }
     * @endcode
     *
     * @param timeoutList A list of @c TemplateRuntimeTimeout type and value pairs
     *
     */
    public static EngineConfiguration createTemplateRuntimeTimeoutConfig( final TemplateRuntimeTimeout[] timeoutList ) {
        return new EngineConfiguration() {
            @Override
            protected long createNativeRef() {
                return createTemplateRuntimeTimeoutConfigBinder( timeoutList );
            }
        };
    }

    // Native Engine JNI methods
    static private native long createTemplateRuntimeTimeoutConfigBinder( TemplateRuntimeTimeout[] timeoutList );

    /**
     * Factory method used to programmatically generate equalizer controller configuration data.
     * This is an optional configuration, and default settings will be used if configuration is not
     * provided. This method produces configuration data according to the JSON structure in the
     * sample below.
     *
     * @code{.json}
     *  "equalizer": {
     *      "bands": {
     *          "BASS": true,
     *          "MIDRANGE": false,
     *          "TREBLE": true
     *      },
     *      "defaultState": {
     *          "bands": {
     *              "BASS": 4,
     *              "TREBLE" : -1
     *          }
     *      },
     *      "minLevel": -6,
     *      "maxLevel": 6
     *  }
     * @endcode
     *
     * The configuration branches are used as follows:
     *
     * @li equalizer.bands: Specifies which bands are supported by the device and will be enabled
     *  for control with Alexa. Each child key is the name of an Alexa-supported band
     *  ("BASS", "MIDRANGE", or "TREBLE") and value is whether the device supports the band. Only
     *  bands explicitly declared supported will be enabled in the SDK and Alexa. Omitting this
     *  branch enables all bands by default.
     *
     * @li equalizer.defaultState: Describes the default or reset state of the equalizer. These
     *  settings are used to reset the equalizer with Alexa such as by saying "Alexa, reset bass."
     *  If this branch or its child is omitted, default values will be used.
     * @li equalizer.defaultState.bands: Defines the default gain level setting in dB for each
     *  supported equalizer band. Each element key is the name of a supported band and value is a
     *  level (int) specifying the default gain in dB. All of the supported bands must be provided
     *  once this branch is defined. All dB levels must obey the limits declared in
     *  "equalizer.minLevel" and "equalizer.maxLevel". Omitting this branch uses the default 0db
     *  for each band.
     *
     * @li equalizer.minLevel and equalizer.maxLevel: Integer values specifying the decibel level
     *  range on which Alexa may operate for the supported bands. The device may support a
     *  different range internally, but Alexa will know only about the limits declared here. Values
     *  should be specified as absolute amplitude gain in integer dB and scaled to the platform's
     *  internal range as necessary. If these values are omitted, the default range min -6dB and
     *  max +6dB will be used.
     *
     * @param  supportedBands The supported equalizer bands. Corresponds to the "equalizer.bands"
     *         config branch. Only bands provided will be enabled. Null @a supportedBands omits the
     *         config branch. Nonnull @a supportedBands includes the branch and declares each band
     *         in the set with a value "true".
     * @param  minLevel The minimum gain level for the equalizer bands in integer dB. Corresponds
     *         to "equalizer.minLevel".
     * @param  maxLevel The maximum gain level for the equalizer bands in integer dB. Corresponds
     *         to "equalizer.maxLevel".
     * @param  defaultBandLevels The default or reset state of the equalizer bands. Corresponds to
     *         the "equalizer.defaultState.bands" config branch. Null @a defaultBandLevels omits
     *         the config branch.
     */
    public static EngineConfiguration createEqualizerControllerConfig( final EqualizerBand[] supportedBands,
                                                                       final int minLevel, final int maxLevel,
                                                                       final EqualizerBandLevel[] defaultBandLevels ) {
       return new EngineConfiguration() {
            @Override
            protected long createNativeRef() {
                return createEqualizerControllerConfigBinder( supportedBands, minLevel, maxLevel, defaultBandLevels );
            }
       };
    }

    // Native Engine JNI methods
    static private native long createEqualizerControllerConfigBinder( EqualizerBand[] supportedBands,
                                                                      int minLevel, int maxLevel,
                                                                      EqualizerBandLevel[] defaultBandLevels );
}