package com.i_echo.lpc.i_echo;


import android.content.Context;
import android.media.AudioManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.HashMap;
import java.util.Locale;

/**
 * Created by LPC-Home1 on 8/1/2015.
 */
public class TTSHelpers {
    private static final String TAG = TTSHelpers.class.getSimpleName();

    protected static final String SPEECH_UTTERANCE_ID = "3";
    protected static final String TICK_UTTERANCE_ID = "5";

    private static TTSHelpers myInstance;
    protected TextToSpeech mTtsInstance;
    protected HashMap<String, String> mSpeechMap = new HashMap<>();
    protected HashMap<String, String> mSoundMap = new HashMap<>();
    protected String beepSound = "beep";
    private Context mContext;

    private TTSHelpers() {
    }

    public static TTSHelpers getInstance() {
        if (null == myInstance) {
            myInstance = new TTSHelpers();
        }
        return myInstance;
    }

    public void init(final Context context) {
        if (null == mTtsInstance) {
            this.mContext = context;
            mTtsInstance = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status != TextToSpeech.ERROR) {
                        mTtsInstance.setLanguage(Locale.US);
                    }
                }
            });
            mTtsInstance.setSpeechRate(1.5f);
            // set up all utterance unique id as 3
            mSpeechMap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, SPEECH_UTTERANCE_ID);
            // set up all tick unique id as 5
            mSoundMap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, TICK_UTTERANCE_ID);
            mTtsInstance.addEarcon("beep", context.getPackageName(), R.raw.sound_yes);
            mTtsInstance.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                    audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);

                }

                @Override
                public void onDone(String utteranceId) {
                }

                @Override
                public void onError(String utteranceId) {
                }
            });
        }
    }

    public boolean isInited() {
        return (mContext != null && mTtsInstance != null);
    }

    /**
     * default, ADD to queue
     * @param message
     */
    public void speak(String message) {
        this.speak(message, TextToSpeech.QUEUE_ADD);
    }

    public void speak(String message, int mode) {
        mTtsInstance.speak(message, mode, mSpeechMap);
    }

    public void playSilence(int duration) {
        mTtsInstance.playSilence(duration, TextToSpeech.QUEUE_ADD, mSpeechMap);
    }

    public void playSilence(int duration, int mode) {
        mTtsInstance.playSilence(duration, mode, mSpeechMap);
    }

    public void playEarcon() {
        playEarcon(TextToSpeech.QUEUE_ADD);
    }

    public void playEarcon(int mode) {
        mTtsInstance.playEarcon(beepSound, mode, mSoundMap);
    }

    public void stop() {
        if (null != mTtsInstance) {
            mTtsInstance.stop();
        }
    }

    public void destroy() {
        if (null != mTtsInstance) {
            mTtsInstance.stop();
            mTtsInstance.shutdown();
            mTtsInstance = null;
        }
    }
}
