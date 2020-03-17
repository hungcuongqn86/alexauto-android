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

package com.amazon.sampleapp.impl.AuthProvider;
import android.app.Activity;
import com.amazon.aace.alexa.AuthProvider;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuthProviderHandler extends AuthProvider implements AuthStateObserver {

    private static final String sTag = "AuthProvider";

    private final Activity mActivity;
    private AuthHandler mAuthHandler;

    private AuthState mAuthState = AuthState.UNINITIALIZED;
    private String mAuthToken = "";

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    public AuthProviderHandler(Activity activity, AuthHandler handler ) {
        mActivity = activity;
        mAuthHandler = handler;
        setupGUI();
    }

    @Override
    public String getAuthToken() {
        return mAuthToken;
    }

    @Override
    public AuthState getAuthState() {
        return mAuthState;
    }

    public void onAuthStateChanged( AuthState state, AuthError error, String token ) {
        mAuthToken = token;
        mAuthState = state;
        mExecutor.execute( new AuthStateChangedRunnable( mAuthState, error));
    }

    private class AuthStateChangedRunnable implements Runnable {
        AuthState state;
        AuthError error;
        AuthStateChangedRunnable( AuthState s, AuthError e){
            state = s;
            error = e;
        }
        public void run() {
            // call to update engine
            authStateChange( state, error );
        }
    }

    private void setupGUI() {

    }

    // After Engine has been started, register this as observer of the auth handler
    public void onInitialize(){
        mAuthHandler.registerAuthStateObserver( this );
    }
}
