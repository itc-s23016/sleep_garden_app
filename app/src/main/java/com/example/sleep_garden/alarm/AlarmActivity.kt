package com.example.sleep_garden.alarm

import android.content.Context
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
import com.example.sleep_garden.MainActivity

class AlarmActivity : ComponentActivity() {

    private val alarmId: String by lazy {
        intent.getStringExtra("alarmId") ?: "default"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ※ 音は AlarmRingtoneService が鳴らす

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

                        // ==== 停止ボタン ====
                        Button(
                            onClick = {
                                // ❌ ここで snoozed=false にしていたのが原因
                                // → STOP では「スヌーズ履歴」を消さない

                                // サービス停止
                                val stop = Intent(
                                    this@AlarmActivity,
                                    AlarmRingtoneService::class.java
                                ).apply {
                                    action = AlarmRingtoneService.ACTION_STOP
                                    putExtra("alarmId", alarmId)
                                }
                                startService(stop)

                                // アプリへ復帰（sleep_active=true なので SleepScreen へ）
                                startActivity(
                                    Intent(this@AlarmActivity, MainActivity::class.java).apply {
                                        addFlags(
                                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                                        )
                                    }
                                )

                                finish()
                            },
                            modifier = Modifier.fillMaxWidth(0.6f)
                        ) {
                            Text("停止")
                        }

                        Spacer(Modifier.height(20.dp))

                        // ==== スヌーズボタン ====
                        Button(
                            onClick = {
                                // 🔥 スヌーズ履歴を保存（このフラグは SleepScreen まで持ち越す）
                                val prefs = getSharedPreferences("sleep_prefs", Context.MODE_PRIVATE)
                                prefs.edit().putBoolean("snoozed", true).apply()

                                // サービスにスヌーズアクション送信（1分後に再度鳴動）
                                sendServiceAction(AlarmRingtoneService.ACTION_SNOOZE)

                                // この画面は閉じる
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

    /** サービスに停止/スヌーズのアクションを送る */
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
