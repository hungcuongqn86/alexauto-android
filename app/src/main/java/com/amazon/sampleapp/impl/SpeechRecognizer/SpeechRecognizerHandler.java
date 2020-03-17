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

package com.amazon.sampleapp.impl.SpeechRecognizer;

import android.app.Activity;

import com.amazon.aace.alexa.SpeechRecognizer;

import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
// AutoVoiceChrome imports

public class SpeechRecognizerHandler extends SpeechRecognizer {

    private static final String TAG = SpeechRecognizerHandler.class.getSimpleName();

    private final Activity mActivity;
    private AudioCueObservable mAudioCueObservable = new AudioCueObservable();
    private final ExecutorService mExecutor = Executors.newFixedThreadPool( 1 );
    private boolean mWakeWordEnabled;
    private boolean mAllowStopCapture = false; // Only true if holdToTalk() returned true
    // AutoVoiceChrome controller

    public SpeechRecognizerHandler( Activity activity,
                                    boolean wakeWordSupported,
                                    boolean wakeWordEnabled ) {
        super( wakeWordSupported && wakeWordEnabled );
        mActivity = activity;
        mWakeWordEnabled = wakeWordEnabled;
    }

    @Override
    public boolean wakewordDetected( String wakeWord ) {
        mAudioCueObservable.playAudioCue( AudioCueState.START_VOICE );

        // Notify Error state to AutoVoiceChrome if disconnected with Alexa

        return true;
    }

    @Override
    public void endOfSpeechDetected() {
        mAudioCueObservable.playAudioCue( AudioCueState.END );
    }

    public void onTapToTalk() {
        if ( tapToTalk() ) mAudioCueObservable.playAudioCue( AudioCueState.START_TOUCH );
    }

    public void onHoldToTalk() {
        mAllowStopCapture = false;
        if ( holdToTalk() ) {
            mAllowStopCapture = true;
            mAudioCueObservable.playAudioCue( AudioCueState.START_TOUCH );
        }
    }

    public void onReleaseHoldToTalk() {
        if ( mAllowStopCapture ) stopCapture();
        mAllowStopCapture = false;
    }

    /* For playing speech recognition audio cues */

    public enum AudioCueState { START_TOUCH, START_VOICE, END }

    public static class AudioCueObservable extends Observable {

        void playAudioCue( AudioCueState state ) {
            setChanged();
            notifyObservers( state );
        }
    }

    public void addObserver( Observer observer ) {
        if ( mAudioCueObservable == null ) mAudioCueObservable = new AudioCueObservable();
        mAudioCueObservable.addObserver( observer );
    }

    // AutoVoiceChrome related functions
}
