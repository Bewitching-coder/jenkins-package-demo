package com.example.kalina_demo

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView


class MainActivity : AppCompatActivity() {

    var t: TestClass?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button?>(R.id.btn_login)?.setOnClickListener {

        }
        // 将Apk上传到老的打包系统
        findViewById<TextView?>(R.id.btn_logout)?.setOnClickListener {

        }
        t = TestClass()
    }
}
