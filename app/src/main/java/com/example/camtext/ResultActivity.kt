package com.example.camtext

import MyDrawView
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.text.TextRecognizer
import kotlinx.android.synthetic.main.activity_result.*
import org.json.JSONArray
import org.json.JSONObject
import kotlin.properties.Delegates


class ResultActivity : AppCompatActivity() {

    private var textRecognizer by Delegates.notNull<TextRecognizer>()
    private lateinit var myDrawView: MyDrawView
    private var ocrResult: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_result)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Share button
        share_btn.setOnClickListener {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, ocr_result.text.toString())
                type = "text/plain"
            }


            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)
        }

        // Clipboard button
        clipboard_btn.setOnClickListener {
            // Get the clipboard system service
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("OCR result", ocr_result.text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Copied to clipboard.", Toast.LENGTH_SHORT).show()
        }

        val extras = intent.extras
        if (extras != null) {
            var imageUri = Uri.parse(extras.getString("imageUri"))

            //set image captured to image view
            image_view.setImageURI(imageUri)
        }


        val parent = image_overlay as FrameLayout
        myDrawView = MyDrawView(this)

        parent.addView(myDrawView)

        myDrawView.layoutParams.width = image_view.layoutParams.width
        myDrawView.layoutParams.height = image_view.layoutParams.height


        doAsync {
            setupOCR()

            val extras = intent.extras
            if (extras != null) {
                var imageUri = Uri.parse(extras.getString("imageUri"))

                val bitmap =
                    MediaStore.Images.Media.getBitmap(applicationContext.contentResolver, imageUri)

                val imFrame = Frame.Builder().setBitmap(bitmap).build()
                val textBlocks = textRecognizer.detect(imFrame)

                val stringBuilder = StringBuilder()
                for (i in 0 until textBlocks.size()) {
                    val textBlock = textBlocks[textBlocks.keyAt(i)]
                    stringBuilder.append(textBlock.value)
                    stringBuilder.append("\n")
                }
                ocrResult = stringBuilder.toString()
                if (ocrResult != "") {
                    extras.getString("imageUri")?.let { addHistoryItem(it, ocrResult) }
                }
            }

            ocr_result.post {
                loading_panel.visibility = View.GONE;
                ocr_result.setText(ocrResult)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_UP -> {
                val res = myDrawView.calculateBoundingBox()
                Log.d("DRAWING", res.toString())
            }
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.option_menu, menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.action_options -> {
                Log.d("MENU", "OPTIONS")
                true
            }
            R.id.action_history -> {
                Log.d("MENU", "HISTORY")
                val myIntent = Intent(applicationContext, HistoryActivity::class.java)
                startActivityForResult(myIntent, 0)
                true
            }
            else -> false
        }
    }

    private fun setupOCR() {
        //  Create text Recognizer
        textRecognizer = TextRecognizer.Builder(this).build()

        if (!textRecognizer.isOperational) {
            Toast.makeText(this, "Dependencies are not loaded yet...please try after few moment!!", Toast.LENGTH_SHORT).show()
            Log.d("OCR","Dependencies are downloading....try after few moment")
            return
        }
    }

    private fun addHistoryItem(uri:String, text:String) {
        var json = JSONArray()

        val sharedPref = getSharedPreferences("appData", Context.MODE_PRIVATE)
        val prefEditor = sharedPref.edit()

        // Retrieve values from preferences
        val str: String? = sharedPref.getString("history", null)
        // Prefs exist, we override the json
        if (str != null) {
            json = JSONArray(str)
        }

        val entry = JSONObject()
        entry.put("Uri", uri)
        entry.put("Text", text)
        json.put(entry)

        prefEditor.putString("history", json.toString())
        prefEditor.apply() // handle writing in the background
    }
}

class doAsync(val handler: () -> Unit) : AsyncTask<Void, Void, Void>() {
    init {
        execute()
    }

    override fun doInBackground(vararg params: Void?): Void? {
        handler()
        return null
    }
}
