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

package com.amazon.aace.cbl.config;

import com.amazon.aace.core.config.EngineConfiguration;

/**
 * A factory interface for creating CBL configuration objects
 */
public class CBLConfiguration {

    private static final String TAG = "CBLConfiguration";

    /**
     * Factory method used to programmatically generate cbl configuration data.
     * The data generated by this method is equivalent to providing the following JSON
     * values in a configuration file:
     *
     * @code    {.json}
     * {
     *   "aace.cbl": {
     *     "requestTimeout": <REQUEST_TIMEOUT_IN_SECONDS> 
     *   }
     * }
     * @endcode
     *
     * @param  requestTimeout The timeout used for requesting code pair
     * The default configuration of 60 seconds will be overriden with this value when configured.
     */
    public static EngineConfiguration createCBLConfig( final int seconds ) {
        return new EngineConfiguration() {
            @Override
            protected long createNativeRef() {
                return createCBLConfigBinder( seconds );
            }
        };
    }

    // Native Engine JNI methods
    static private native long createCBLConfigBinder( int seconds );

    /**
     * Factory method used to programmatically generate cbl configuration data.
     * The data generated by this method is equivalent to providing the following JSON
     * values in a configuration file:
     *
     * @code    {.json}
     * {
     *   "aace.cbl": {
     *     "enableUserProfile": <true/false>
     *   }
     * }
     * @endcode
     *
     * @param [in] enableUserProfile Enable functionality to request user profile
     */
    public static EngineConfiguration createCBLUserProfileConfig( final boolean enableUserProfile ) {
        return new EngineConfiguration() {
            @Override
            protected long createNativeRef() {
                return createCBLUserProfileConfigBinder( enableUserProfile );
            }
        };
    }

    static private native long createCBLUserProfileConfigBinder( boolean enableUserProfile );
}
