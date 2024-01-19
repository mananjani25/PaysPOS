package com.pays.pos.utils.scanner.helpers

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.IBinder
import com.pays.pos.utils.scanner.helpers.BackgroundSoundService
import java.io.IOException
import java.lang.Exception
import java.lang.UnsupportedOperationException

/**
 * Service class to handle the audio alarm when virtual tether event occurs
 */
class BackgroundSoundService : Service() {
    var audioAlarmPlayer: MediaPlayer? = null
    var context: Context? = null
    var afChangeListener: AudioManager.OnAudioFocusChangeListener? = null
    private var length = 0
    override fun onBind(intent: Intent): IBinder? {
        // TODO: Return the communication channel to the service.
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        //TODO scanner
        ///playMusic(R.raw.vt_alarm_tone);
        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        super.onDestroy()
        if (audioAlarmPlayer != null) {
            try {
                audioAlarmPlayer!!.stop()
                audioAlarmPlayer!!.release()
            } finally {
                audioAlarmPlayer = null
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        stopSelf()
    }

    /**
     * custom method to play alarm sounds for application
     * @param musicFile alarm sound raw audio file
     */
    fun playMusic(musicFile: Int) {
        if (audioAlarmPlayer != null) {
            if (audioAlarmPlayer!!.isPlaying) {
                try {
                    audioAlarmPlayer!!.stop()
                    audioAlarmPlayer!!.release()
                    audioAlarmPlayer = MediaPlayer.create(this, musicFile)
                    val am = applicationContext.getSystemService(AUDIO_SERVICE) as AudioManager
                    val result = am.requestAudioFocus(
                        afChangeListener,
                        AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN
                    )
                    if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                        // Start playback.
                        audioAlarmPlayer?.setLooping(true)
                        val volume = (1 - Math.log((MAX_VOLUME - 85).toDouble()) / Math.log(
                            MAX_VOLUME.toDouble()
                        )).toFloat()
                        audioAlarmPlayer?.setVolume(volume, volume)
                        audioAlarmPlayer?.start()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                try {
                    audioAlarmPlayer = MediaPlayer.create(this, musicFile)
                    val am = applicationContext.getSystemService(AUDIO_SERVICE) as AudioManager
                    val result = am.requestAudioFocus(
                        afChangeListener,
                        AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN
                    )
                    if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                        // Start playback.
                        audioAlarmPlayer?.setLooping(true)
                        val volume = (1 - Math.log((MAX_VOLUME - 85).toDouble()) / Math.log(
                            MAX_VOLUME.toDouble()
                        )).toFloat()
                        audioAlarmPlayer?.setVolume(volume, volume)
                        audioAlarmPlayer?.prepare()
                        audioAlarmPlayer?.start()
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        } else {
            try {
                audioAlarmPlayer = MediaPlayer.create(this, musicFile)
                val am = applicationContext.getSystemService(AUDIO_SERVICE) as AudioManager
                val result = am.requestAudioFocus(
                    afChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    // Start playback.
                    audioAlarmPlayer?.setLooping(true)
                    val volume = (1 - Math.log((MAX_VOLUME - 85).toDouble()) / Math.log(
                        MAX_VOLUME.toDouble()
                    )).toFloat()
                    audioAlarmPlayer?.setVolume(volume, volume)
                    audioAlarmPlayer?.start()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * This method is to pause alarm sound
     */
    fun pauseMusic() {
        if (audioAlarmPlayer!!.isPlaying) {
            audioAlarmPlayer!!.pause()
            length = audioAlarmPlayer!!.currentPosition
        }
    }

    /**
     * This method is to resume alarm sound
     */
    fun resumeMusic() {
        if (audioAlarmPlayer!!.isPlaying == false) {
            audioAlarmPlayer!!.seekTo(length)
            audioAlarmPlayer!!.start()
        }
    }

    /**
     * This method is to stop alarm sound
     */
    fun stopMusic() {
        audioAlarmPlayer!!.stop()
        audioAlarmPlayer!!.release()
        audioAlarmPlayer = null
    }

    /**
     * Error handling for the sound play service
     * @param mediaPlayer media player object
     * @param errorCode error code
     * @return status of the media player
     */
    fun onError(mediaPlayer: MediaPlayer?, errorCode: Int): Boolean {
        if (audioAlarmPlayer != null) {
            try {
                audioAlarmPlayer!!.stop()
                audioAlarmPlayer!!.release()
            } finally {
                audioAlarmPlayer = null
            }
        }
        return false
    }

    companion object {
        private const val MAX_VOLUME = 100
    }
}