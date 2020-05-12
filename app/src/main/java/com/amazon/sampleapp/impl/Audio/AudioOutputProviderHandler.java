package com.amazon.sampleapp.impl.Audio;

import android.app.Activity;

import com.amazon.sampleapp.aace.audio.AudioOutput;
import com.amazon.sampleapp.aace.audio.AudioOutputProvider;

import java.util.HashMap;

public class AudioOutputProviderHandler extends AudioOutputProvider
{
    private static final String sTag = AudioOutputProviderHandler.class.getSimpleName();

    private final Activity mActivity;

    private HashMap<String,AudioOutput> mAudioOutputMap;

    public AudioOutputProviderHandler(  Activity activity ) {
        mActivity = activity;
        mAudioOutputMap = new HashMap<>();
    }

    public AudioOutput getOutputChannel( String name ) {
        return mAudioOutputMap.containsKey( name ) ? mAudioOutputMap.get( name ) : null;
    }

    @Override
    public AudioOutput openChannel( String name, AudioOutputType type )
    {
        AudioOutput audioOutputChannel = null;

        switch( type )
        {
            case COMMUNICATION:
                audioOutputChannel = new RawAudioOutputHandler( mActivity, name );
                break;

            default:
                audioOutputChannel = new AudioOutputHandler( mActivity, name );
                break;
        }

        mAudioOutputMap.put( name, audioOutputChannel );

        return audioOutputChannel;
    }
}
