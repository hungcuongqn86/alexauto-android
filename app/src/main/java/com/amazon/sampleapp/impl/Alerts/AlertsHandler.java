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

package com.amazon.sampleapp.impl.Alerts;

import android.app.Activity;
import android.widget.TextView;

import com.amazon.sampleapp.aace.alexa.Alerts;

public class AlertsHandler extends Alerts {

    private static final String sTag = "Alerts";

    private final Activity mActivity;
    private TextView mStateText;

    public AlertsHandler( Activity activity) {
        mActivity = activity;
    }

    @Override
    public void alertStateChanged( final String alertToken,
                                   final AlertState state,
                                   final String reason ) {
        mActivity.runOnUiThread( new Runnable() {
            @Override
            public void run() { mStateText.setText( state != null ? state.toString() : "" ); }
        });
    }

    @Override
    public void alertCreated( String alertToken, String detailedInfo ) {
    }

    @Override
    public void alertDeleted( String alertToken ) {
    }

    private void onLocalStop() {
        super.localStop();
    }

    private void onRemoveAllAlerts( ) {
        super.removeAllAlerts();
    }
}
