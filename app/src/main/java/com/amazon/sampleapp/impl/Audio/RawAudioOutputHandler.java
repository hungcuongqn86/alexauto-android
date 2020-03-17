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

package com.amazon.sampleapp.impl.Audio;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.support.annotation.Nullable;

import com.amazon.aace.audio.AudioOutput;
import com.amazon.aace.audio.AudioStream;

/**
 * A @c AudioOutput capable to play raw PCM 16 bit data @ 16 KHZ.
 */
public class RawAudioOutputHandler extends AudioOutput {

    private static final String sTag = "RawAudioAudioOutputHandler";

    private final Activity mActivity;
    private final String mName;
    private AudioTrack mAudioTrack;
    private Thread mAudioPlaybackThread;
    private float mVolume = 0.5f;
    private MutedState mMutedState = MutedState.UNMUTED;
    private AudioStream mAudioStream;

    public RawAudioOutputHandler(
        Activity activity,
        String name ) {
        mActivity = activity;
        mName = name;

        initializePlayer();
    }

    private void initializePlayer() {
        int audioBufferSize = AudioTrack.getMinBufferSize(
            16000,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT);
        mAudioTrack = new AudioTrack(
            AudioManager.STREAM_VOICE_CALL,
            16000,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            audioBufferSize,
            AudioTrack.MODE_STREAM);
        if (mAudioTrack.getState() == AudioTrack.STATE_UNINITIALIZED) {
            throw new RuntimeException("Failed to create AudioTrack");
        }
    }

    private void resetPlayer() {
        mAudioTrack.flush();
    }

    public boolean isPlaying() {
        return mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING;
    }

    //
    // Handle playback directives from Engine
    //

    @Override
    public boolean prepare( AudioStream stream, boolean repeating ) {
        mAudioStream = stream;
        resetPlayer();
        return true;
    }

    @Override
    public boolean prepare( String url, boolean repeating ) {
        throw new RuntimeException("URL based playback not supported " + url);
    }

    @Override
    public boolean play() {
        mAudioTrack.play();
        mAudioPlaybackThread = new Thread(new AudioSampleReadWriteRunnable());
        mAudioPlaybackThread.start();
        return true;
    }

    @Override
    public boolean stop() {
        mAudioTrack.stop();
        return true;
    }

    @Override
    public boolean pause() {
        mAudioTrack.pause();
        return true;
    }

    @Override
    public boolean resume() {
        mAudioTrack.play();
        return true;
    }

    @Override
    public boolean setPosition( long position ) {
        return true;
    }

    @Override
    public long getPosition() { return Math.abs( mAudioTrack.getPlaybackHeadPosition() ); }

    //
    // Handle state changes and notify Engine
    //

    private void onPlaybackStarted () {
        mediaStateChanged( MediaState.PLAYING );
    }

    private void onPlaybackStopped () {
        mediaStateChanged( MediaState.STOPPED );
    }

    private class AudioSampleReadWriteRunnable implements Runnable {
        @Override
        public void run() {
            onPlaybackStarted();

            try {
                byte[] audioBuffer = new byte[640];
                while (isPlaying() && !mAudioStream.isClosed()) {
                    int dataRead = mAudioStream.read(audioBuffer);
                    if (dataRead > 0) {
                        mAudioTrack.write(audioBuffer, 0, dataRead);
                    }
                }
            } catch (Exception exp) {
                String message = exp.getMessage() != null ? exp.getMessage() : "";
                mediaError(MediaError.MEDIA_ERROR_UNKNOWN, message);
            } finally {
                onPlaybackStopped();
            }
        }
    }

    @Override
    public boolean volumeChanged( float volume ) {
        if(mVolume == volume)
            return true;
        mVolume = volume;
        if ( mMutedState == MutedState.MUTED ) {
            mAudioTrack.setVolume( 0 );
        } else {
            mAudioTrack.setVolume( mVolume );
        }
        return true;
    }

    @Override
    public boolean mutedStateChanged( MutedState state ) {
        if( state != mMutedState ) {
            mAudioTrack.setVolume( state == MutedState.MUTED ? 0 : mVolume );
            mMutedState = state;
        }

        return true;
    }
}
