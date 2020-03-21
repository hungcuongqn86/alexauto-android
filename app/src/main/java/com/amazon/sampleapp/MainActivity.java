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

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

public class MainActivity extends AppCompatActivity {
    /* Speech Recognition Components */
    private boolean mIsTalkButtonLongPressed = false;
    private MenuItem mTapToTalkIcon;

    private AsvAlexaPlugin asvAlexaPlugin = new AsvAlexaPlugin();

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
        if (mTapToTalkIcon != null) {
            mTapToTalkIcon.setActionView(R.layout.menu_item_talk);
            // Set hold-to-talk action
            mTapToTalkIcon.getActionView().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    asvAlexaPlugin.tapToTalk();
                }
            });

            /*
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
            */
        }
    }
}
