package knf.tools.bypass

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import knf.kuma.uagen.randomUA
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startBypass(666,"https://kissasian.sh/",false)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 666 && resultCode == Activity.RESULT_OK){
            Log.e("Bypass created","\nCookies: ${data?.getStringExtra("cookies")}\nUser agent: ${data?.getStringExtra("user_agent")}")
        }
    }
}