package com.example.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

enum class FeedbackType {
    ADD,
    REMOVE,
    ASK,
    WARN
}

class SoundFeedback(private val context: Context) {

    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
        } catch (e: Exception) {
            toneGenerator = null
        }
    }

    fun play(type: FeedbackType, soundEnabled: Boolean) {
        if (!soundEnabled) return

        try {
            when (type) {
                FeedbackType.ADD -> {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
                    vibrate(40)
                }
                FeedbackType.REMOVE -> {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 120)
                    vibratePattern(longArrayOf(0, 30, 40, 30))
                }
                FeedbackType.ASK -> {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 100)
                    vibrate(60)
                }
                FeedbackType.WARN -> {
                    toneGenerator?.startTone(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, 200)
                    vibratePattern(longArrayOf(0, 80, 50, 80))
                }
            }
        } catch (e: Exception) {
            // Ignore any transient audio errors
        }
    }

    private fun vibrate(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.vibrate(CombinedVibration.createParallel(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(durationMs)
                }
            }
        } catch (e: Exception) {
            // Ignore vibration permission or hardware exceptions
        }
    }

    private fun vibratePattern(timings: LongArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                v?.vibrate(VibrationEffect.createWaveform(timings, -1))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                v?.vibrate(timings, -1)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
            // Ignore
        }
    }
}
