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
import com.example.sleep_garden.setSnoozed    // ★ ここ重要（MainActivity側の関数）

class AlarmActivity : ComponentActivity() {

    private val alarmId: String by lazy {
        intent.getStringExtra("alarmId") ?: "default"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //（音は AlarmRingtoneService が鳴らす）

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

                        /* ------------------ 停止ボタン ------------------ */
                        Button(
                            onClick = {
                                // 1) 鳴動サービスを停止
                                val stop = Intent(this@AlarmActivity, AlarmRingtoneService::class.java).apply {
                                    action = AlarmRingtoneService.ACTION_STOP
                                    putExtra("alarmId", alarmId)
                                }
                                startService(stop)

                                // 2) MainActivity に戻る
                                startActivity(
                                    Intent(this@AlarmActivity, com.example.sleep_garden.MainActivity::class.java).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                    }
                                )

                                finish()
                            },
                            modifier = Modifier.fillMaxWidth(0.6f)
                        ) {
                            Text("停止")
                        }

                        Spacer(Modifier.height(20.dp))

                        /* ------------------ スヌーズボタン ------------------ */
                        Button(
                            onClick = {
                                // ★★★ この一行が超重要！！ ★★★
                                setSnoozed(this@AlarmActivity, true)

                                // サービスにスヌーズ指示
                                sendServiceAction(AlarmRingtoneService.ACTION_SNOOZE)

                                // 画面閉じる
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

    /** サービスへ停止/スヌーズのアクションを送る */
    private fun sendServiceAction(action: String) {
        val intent = Intent(this, AlarmRingtoneService::class.java).apply {
            this.action = action
            putExtra("alarmId", alarmId)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, intent)
        } else {
            startService(intent)
        }
    }
}
