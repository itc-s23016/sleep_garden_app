package com.example.sleep_garden.alarm

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class AlarmActivity : ComponentActivity() {

    private val alarmId: String by lazy {
        intent.getStringExtra("alarmId") ?: "default"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ※ 音は AlarmRingtoneService が鳴らす。ここでは鳴らさない。

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
                        Spacer(Modifier.height(40.dp))

                        Button(
                            onClick = {
                                sendServiceAction(AlarmRingtoneService.ACTION_STOP)
                                finish()
                            },
                            modifier = Modifier.fillMaxWidth(0.6f)
                        ) { Text("停止") }

                        Spacer(Modifier.height(20.dp))

                        Button(
                            onClick = {
                                sendServiceAction(AlarmRingtoneService.ACTION_SNOOZE)
                                finish()
                            },
                            modifier = Modifier.fillMaxWidth(0.6f)
                        ) { Text("スヌーズ（1分後）") }
                    }
                }
            }
        }
    }

    /** サービスに停止/スヌーズのアクションを送る */
    private fun sendServiceAction(action: String) {
        val intent = Intent(this, AlarmRingtoneService::class.java).apply {
            this.action = action
            putExtra("alarmId", alarmId)
        }
        // O+ は startForegroundService、それ未満は startService
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, intent)
        } else {
            startService(intent)
        }
    }
}
