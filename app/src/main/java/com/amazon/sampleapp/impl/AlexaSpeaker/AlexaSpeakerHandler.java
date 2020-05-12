package com.amazon.sampleapp.impl.AlexaSpeaker;

import android.app.Activity;
import android.widget.SeekBar;
import android.widget.TextView;

import com.amazon.sampleapp.aace.alexa.AlexaSpeaker;

public class AlexaSpeakerHandler extends AlexaSpeaker
{
    private static final String TAG = AlexaSpeaker.class.getSimpleName();

    private final Activity mActivity;

    private boolean mIsMuted = false;
    private byte mAlexaVolume = 50;
    private byte mAlertsVolume = 50;

    public AlexaSpeakerHandler( Activity activity) {
        mActivity = activity;
    }

    @Override
    public void speakerSettingsChanged( SpeakerType type, boolean local, byte volume, boolean mute )
    {
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
