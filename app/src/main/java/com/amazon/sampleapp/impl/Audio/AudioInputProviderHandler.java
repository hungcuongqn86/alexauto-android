package com.amazon.sampleapp.impl.Audio;

import android.app.Activity;

import com.amazon.aace.audio.AudioInput;
import com.amazon.aace.audio.AudioInputProvider;

public class AudioInputProviderHandler extends AudioInputProvider
{
    private static final String sTag = "AudioInputProviderHandler";

    private AudioInput mDefaultAudioInput = null;
    private final Activity mActivity;

    public AudioInputProviderHandler(  Activity activity) {
        mActivity = activity;
    }

    @Override
    public AudioInput openChannel( String name, AudioInputType type )
    {
        if( type == AudioInputType.VOICE || type == AudioInputType.COMMUNICATION ) {
            return getDefaultAudioInput();
        }
        else {
            return null;
        }
    }

    private AudioInput getDefaultAudioInput() {
        if( mDefaultAudioInput == null ) {
            mDefaultAudioInput = new AudioInputHandler( mActivity );
        }
        return mDefaultAudioInput;
    }
}
