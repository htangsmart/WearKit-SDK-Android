package com.topstep.wearkit.sample.utils;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;

import com.topstep.wearkit.sample.MyApplication;

public class VibratorUtil {
    protected static AudioManager audioManager;
    private static MediaPlayer mMediaPlayer = null;
    protected static Vibrator vibrator;
    private static int volume = 0; //上次音量

    static {
        audioManager = (AudioManager) MyApplication.getInstance().getSystemService(Context.AUDIO_SERVICE); //此方法是由Context调用的
        vibrator = (Vibrator) MyApplication.getInstance().getSystemService(Context.VIBRATOR_SERVICE); //同上
    }

    /**
     * play system ringtone  播放系统铃声
     */
    public static void vibrateAndPlayTone() {
        try {
            if (mMediaPlayer == null) {
                Uri notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                mMediaPlayer = MediaPlayer.create(MyApplication.getInstance(), notificationUri);
                mMediaPlayer.setLooping(true);
                mMediaPlayer.start();
            }
            volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

            audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    /*BuildConfig.DEBUG ? 4 :*/ audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                    AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE
            );
            long[] pattern = new long[]{1000, 800, 1000, 800};
            vibrator.vibrate(pattern, 0); //震动
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * stop play  停止播放
     */
    public static void stopVibrator() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        if (volume != 0) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        }
        if (vibrator != null) {
            vibrator.cancel();
        }
    }
}
