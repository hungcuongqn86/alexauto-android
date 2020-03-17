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

package com.amazon.sampleapp.impl.PlaybackController;

import android.app.Activity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.amazon.aace.alexa.PlaybackController;
import com.amazon.aace.audio.AudioOutput;

import java.util.Formatter;
import java.util.Locale;

public class PlaybackControllerHandler extends PlaybackController {

    private static final String sTag = PlaybackControllerHandler.class.getSimpleName();

    private final Activity mActivity;
    private final StringBuilder mStringBuilder;
    private final Formatter mFormatter;
    private ImageButton mControlPrev, mControlNext, mControlSkipForward, mControlSkipBackward;
    private ToggleButton mControlPlayPause, mShuffleToggle, mLoopToggle, mRepeatToggle, mThumbsUpToggle, mThumbsDownToggle;
    private TextView mProgressTime, mEndTime, mTitle, mArtist, mProvider;
    private ProgressBar mProgress;
    private String mCurrentProvider = "";
    private long mCurrentDuration = AudioOutput.TIME_UNKNOWN;

    public PlaybackControllerHandler( Activity activity) {
        mActivity = activity;
        mStringBuilder = new StringBuilder();
        mFormatter = new Formatter( mStringBuilder, Locale.US );
    }

    public void setPlayerInfo( final String title, final String artist, final String provider ) {
        mCurrentProvider = provider;
        mActivity.runOnUiThread( new Runnable() {
            @Override
            public void run() {
                mTitle.setText( title );
                mArtist.setText( artist );
                mProvider.setText( provider );
            }
        });
    }

    public String getProvider(){
        return mCurrentProvider;
    }

    public void start() {
        mActivity.runOnUiThread( new Runnable() {
            @Override
            public void run() {
                mControlPrev.setEnabled( true );
                mControlPlayPause.setEnabled( true );
                mControlPlayPause.setChecked(true);
                mControlNext.setEnabled( true );
                mProgress.setMax( 1000 );
            }
        });
    }

    public void stop() {
        mActivity.runOnUiThread( new Runnable() {
            @Override
            public void run() {
                mControlPlayPause.setChecked(false);
            }
        });
    }

    public void reset() {
        mActivity.runOnUiThread( new Runnable() {
            @Override
            public void run() {
                mControlPlayPause.setChecked(false);
                mControlPrev.setEnabled( false );
                mControlPlayPause.setEnabled( false );
                mControlNext.setEnabled( false );
                resetProgress();
                setPlayerInfo( "", "", "" );
            }
        });
    }

    // Updates Control Button's states
    public void updateControlButton( final String name, final boolean enabled ) {
        mActivity.runOnUiThread( new Runnable() {
            @Override
            public void run() {
                switch ( name ) {
                    case "PREVIOUS":
                        mControlPrev.setEnabled( enabled );
                        break;
                    case "PLAY_PAUSE":
                        mControlPlayPause.setEnabled( enabled );
                        break;
                    case "NEXT":
                        mControlNext.setEnabled( enabled );
                        break;
                    case "SKIP_FORWARD":
                        mControlSkipForward.setVisibility( View.VISIBLE );
                        mControlSkipForward.setEnabled( enabled );
                        break;
                    case "SKIP_BACKWARD":
                        mControlSkipBackward.setVisibility( View.VISIBLE );
                        mControlSkipBackward.setEnabled( enabled );
                        break;
                }
            }
        });
    }

    // Updates Toggle's display states
    // NOTE: Disabled controls not hidden here for development visibility.
    public void updateControlToggle( final String name, final boolean enabled, final boolean selected ) {
        mActivity.runOnUiThread( new Runnable() {
            @Override
            public void run() {
                switch ( name ) {
                    case "SHUFFLE":
                        mShuffleToggle.setVisibility( View.VISIBLE );
                        mShuffleToggle.setEnabled( enabled );
                        mShuffleToggle.setChecked( selected );
                        break;
                    case "LOOP":
                        mLoopToggle.setVisibility( View.VISIBLE );
                        mLoopToggle.setEnabled( enabled );
                        mLoopToggle.setChecked( selected );
                        break;
                    case "REPEAT":
                        mRepeatToggle.setVisibility( View.VISIBLE );
                        mRepeatToggle.setEnabled( enabled );
                        mRepeatToggle.setChecked( selected );
                        break;
                    case "THUMBS_UP":
                        mThumbsUpToggle.setVisibility( View.VISIBLE );
                        mThumbsUpToggle.setEnabled( enabled );
                        mThumbsUpToggle.setChecked( selected );
                        break;
                    case "THUMBS_DOWN":
                        mThumbsDownToggle.setVisibility( View.VISIBLE );
                        mThumbsDownToggle.setEnabled( enabled );
                        mThumbsDownToggle.setChecked( selected );
                        break;
                }
            }
        });
    }

    public void hidePlayerInfoControls(){
        mActivity.runOnUiThread( new Runnable() {
            @Override
            public void run() {
                mControlSkipForward.setVisibility( View.GONE );
                mControlSkipBackward.setVisibility( View.GONE );

                mShuffleToggle.setVisibility( View.GONE );
                mLoopToggle.setVisibility( View.GONE );
                mRepeatToggle.setVisibility( View.GONE );
                mThumbsUpToggle.setVisibility( View.GONE );
                mThumbsDownToggle.setVisibility( View.GONE );
            }
        });
    }

    private void resetProgress() {
        mActivity.runOnUiThread( new Runnable() {
            @Override
            public void run() {
                mProgress.setProgress( 0 );
                mProgressTime.setText( stringForTime( AudioOutput.TIME_UNKNOWN )  );
                mEndTime.setText( stringForTime( AudioOutput.TIME_UNKNOWN ) );
            }
        });
    }

    public void setTime( long position, long duration )
    {
        if( mCurrentDuration != duration )
        {
            mEndTime.setText( stringForTime( duration ) );
            mCurrentDuration = duration;
        }
        else
        {
            mProgress.setProgress( (int) (1000L * position / duration) );
        }

        mProgressTime.setText( stringForTime( position ) );
    }

    private String stringForTime( long timeMs )
    {
        if( timeMs == AudioOutput.TIME_UNKNOWN ) {
            return "-:--";
        }

        int totalSeconds = (int) timeMs / 1000;
        int seconds = totalSeconds % 60;
        int minutes = ( totalSeconds / 60 ) % 60;
        int hours   = totalSeconds / 3600;

        mStringBuilder.setLength( 0 );
        if ( hours > 0 ) {
            return mFormatter.format( "%d:%02d:%02d", hours, minutes, seconds ).toString();
        } else {
            return mFormatter.format( "%02d:%02d", minutes, seconds ).toString();
        }
    }
}
