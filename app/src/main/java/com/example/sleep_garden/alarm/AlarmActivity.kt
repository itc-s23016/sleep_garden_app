package com.example.sleep_garden.alarm

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class AlarmActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startAlarmSound()

        setContent {
            MaterialTheme {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "⏰ アラームが鳴っています！",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(40.dp))
                        Button(
                            onClick = {
                                stopAlarmSound()
                                finish()
                            },
                            modifier = Modifier.fillMaxWidth(0.6f)
                        ) {
                            Text("停止")
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                stopAlarmSound()
                                snoozeAlarm(this@AlarmActivity)
                                finish()
                            },
                            modifier = Modifier.fillMaxWidth(0.6f)
                        ) {
                            Text("スヌーズ（1分後）")
                        }
                    }
                }
            }
        }
    }

    private fun startAlarmSound() {
        try {
            val alarmUri: Uri = Settings.System.DEFAULT_ALARM_ALERT_URI
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmActivity, alarmUri)
                setAudioStreamType(AudioManager.STREAM_ALARM)
                isLooping = true
                prepare()
                setVolume(1.0f, 1.0f)
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAlarmSound() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun snoozeAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            200,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + 1 * 60 * 1000 // 1分後
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarmSound()
    }
    companion object {
        private var staticMediaPlayer: MediaPlayer? = null

        fun startAlarmSoundStatic(context: Context) {
            try {
                val alarmUri: Uri = Settings.System.DEFAULT_ALARM_ALERT_URI
                staticMediaPlayer = MediaPlayer().apply {
                    setDataSource(context, alarmUri)
                    setAudioStreamType(AudioManager.STREAM_ALARM)
                    isLooping = true
                    prepare()
                    setVolume(1.0f, 1.0f)
                    start()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun stopAlarmSoundStatic() {
            staticMediaPlayer?.stop()
            staticMediaPlayer?.release()
            staticMediaPlayer = null
        }
    }
}
