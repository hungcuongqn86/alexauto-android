package com.amazon.sampleapp.impl.AlexaSpeaker;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.amazon.aace.alexa.AlexaSpeaker;
import com.amazon.sampleapp.R;
import com.amazon.sampleapp.impl.Logger.LoggerHandler;

public class AlexaSpeakerHandler extends AlexaSpeaker
{
    private static final String TAG = AlexaSpeaker.class.getSimpleName();

    private final Activity mActivity;
    private final LoggerHandler mLogger;

    private boolean mIsMuted = false;
    private byte mAlexaVolume = 50;
    private byte mAlertsVolume = 50;

    public AlexaSpeakerHandler( Activity activity, LoggerHandler logger ) {
        mActivity = activity;
        mLogger = logger;
    }

    @Override
    public void speakerSettingsChanged( SpeakerType type, boolean local, byte volume, boolean mute )
    {
        mLogger.postInfo( TAG, String.format( "speakerSettingsChanged [type=%s,local=%b,volume=%d,mute=%b]", type.toString(), local, volume, mute ));

        if( type == SpeakerType.ALEXA_VOLUME ) {
            mAlexaVolume = volume;
            mIsMuted = mute;
        }
        else if( type == SpeakerType.ALERTS_VOLUME ) {
            mAlertsVolume = volume;
        }
    }

    //
    // GUI
    //
    private SeekBar mAlexaVolumeControl;
    private SeekBar mAlertsVolumeControl;
    private TextView mMuteButton;
}
