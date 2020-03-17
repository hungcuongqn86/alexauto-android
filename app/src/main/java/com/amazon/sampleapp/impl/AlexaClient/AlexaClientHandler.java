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

package com.amazon.sampleapp.impl.AlexaClient;

import android.app.Activity;
import android.widget.TextView;

import com.amazon.aace.alexa.AlexaClient;
import com.amazon.sampleapp.R;

public class AlexaClientHandler extends AlexaClient {

    private static final String TAG = AlexaClientHandler.class.getSimpleName();

    private final Activity mActivity;
    private TextView mConnectionText, mAuthText, mDialogText;
    private ConnectionStatus mConnectionStatus = ConnectionStatus.DISCONNECTED;
    // AutoVoiceChrome controller

    public AlexaClientHandler( Activity activity) {
        mActivity = activity;
        setupGUI();
    }

    @Override
    public void dialogStateChanged( final DialogState state ) {
        mActivity.runOnUiThread( new Runnable() {
            @Override
            public void run() {
                mDialogText.setText( state != null ? state.toString() : "" );
            }
        });
    }

    @Override
    public void authStateChanged( final AuthState state, final AuthError error ) {
        mActivity.runOnUiThread( new Runnable() {
            @Override
            public void run() {
                mAuthText.setText( state != null ? state.toString() : "" );
            }
        });
    }

    @Override
    public void connectionStatusChanged( final ConnectionStatus status,
                                         final ConnectionChangedReason reason ) {
        mConnectionStatus = status;
        mActivity.runOnUiThread( new Runnable() {
            @Override
            public void run() {
                mConnectionText.setText( status != null ? status.toString() : "" );
            }
        });

        // Notify error state change to AutoVoiceChrome
    }

    public ConnectionStatus getConnectionStatus () { return mConnectionStatus; }

    private void setupGUI() {
        mConnectionText = mActivity.findViewById( R.id.connectionState );
        mAuthText = mActivity.findViewById( R.id.authState );
        mDialogText = mActivity.findViewById( R.id.dialogState );

        mConnectionText.setText( AlexaClient.ConnectionStatus.DISCONNECTED.toString() );
        mAuthText.setText( AlexaClient.AuthState.UNINITIALIZED.toString() );
        mDialogText.setText(AlexaClient.DialogState.IDLE.toString() );
    }
    // AutoVoiceChrome related functions
}