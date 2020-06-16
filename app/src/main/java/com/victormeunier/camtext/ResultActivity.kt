package com.victormeunier.camtext

import MyDrawView
import android.content.*
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.text.TextRecognizer
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_result.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.properties.Delegates


class ResultActivity : AppCompatActivity() {

    private val handler: Handler = Handler()
    private var textRecognizer by Delegates.notNull<TextRecognizer>()
    private lateinit var myDrawView: MyDrawView
    private var ocrResult: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_result)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // This will disable the Soft Keyboard from appearing by default
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

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

            val imgResource: Int = R.drawable.clipboard_done_24
            clipboard_btn.setCompoundDrawablesWithIntrinsicBounds(0, imgResource, 0, 0)
            clipboard_btn.text = resources.getString(R.string.copied)
            clipboard_btn.invalidate()

            Snackbar.make(result_main, resources.getString(R.string.copied_toast), Snackbar.LENGTH_SHORT).show()
            //Toast.makeText(this, resources.getString(R.string.copied_toast), Toast.LENGTH_SHORT).show()
        }

        // Translate button
        translate_btn.setOnClickListener {

        }

        // Capture button
        recapture_btn.setOnClickListener {
            val myIntent = Intent(applicationContext, MainActivity::class.java)
            startActivityForResult(myIntent, 0)
        }

        val extras = intent.extras
        if (extras != null) {
            var imageUri = Uri.parse(extras.getString("imageUri"))
            //set image captured to image view
            /*
            val bitmap: Bitmap = BitmapFactory.decodeFile(imageUri.path)
            if (bitmap != null) image_view.setImageBitmap(bitmap) else {
                image_view.setImageResource(R.drawable.image_placeholder)
            }
            */
            try {
                image_view.setImageURI(imageUri)
            } catch (e: Throwable) {
                image_view.setImageResource(R.drawable.image_placeholder)
            }
            /*
            val file = File(imageUri.path)
            if (file.exists()) {
                //set image captured to image view
                image_view.setImageURI(imageUri)
            }
            else
            {
                image_view.setImageResource(R.drawable.image_placeholder)
            }
            */
        }

        // TODO: handle text selection
        /*
        val parent = image_overlay as FrameLayout
        myDrawView = MyDrawView(this)

        parent.addView(myDrawView)

        myDrawView.layoutParams.width = image_view.layoutParams.width
        myDrawView.layoutParams.height = image_view.layoutParams.height
        */

        // There's an extra "text" when launching this activity from
        // the history list. We don't need to redo OCR
        if (extras?.getString("text") != null){
            ocr_result.setText(extras?.getString("text"))
            loading_anim.visibility = View.GONE;
            return
        }

        DoAsync {
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
                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this /* Activity context */)
                if (ocrResult != "" && sharedPreferences.getBoolean("history", true)) {
                    extras.getString("imageUri")?.let { addHistoryItem(it, ocrResult) }
                }
            }

            ocr_result.post {
                loading_anim.visibility = View.GONE;
                ocr_result.setText(ocrResult)
            }
        }

        // TODO: handle text selection
        //handler.postDelayed(runnable, 2000); // Call the handler for the first time.
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
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
                val myIntent = Intent(applicationContext, SettingsActivity::class.java)
                startActivityForResult(myIntent, 0)
                true
            }
            R.id.action_history -> {
                Log.d("MENU", "HISTORY")
                val myIntent = Intent(applicationContext, HistoryActivity::class.java)
                startActivityForResult(myIntent, 0)
                true
            }
            R.id.action_about -> {
                val myIntent = Intent(applicationContext, AboutActivity::class.java)
                startActivityForResult(myIntent, 0)
                true
            }
            R.id.action_rate -> {
                rateMyApp()
                true
            }
            else -> false
        }
    }

    private fun rateMyApp() {
        val uri: Uri = Uri.parse("market://details?id=" + applicationContext.getPackageName())
        val goToMarket = Intent(Intent.ACTION_VIEW, uri)
        // To count with Play market backstack, After pressing back button,
        // to taken back to our application, we need to add following flags to intent.
        goToMarket.addFlags(
            Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        )
        try {
            startActivity(goToMarket)
        } catch (e: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=" + applicationContext.getPackageName())
                )
            )
        }
    }

    private fun setupOCR() {
        //  Create text Recognizer
        textRecognizer = TextRecognizer.Builder(this).build()

        if (!textRecognizer.isOperational) {
            //Toast.makeText(this, "Dependencies are not loaded yet...please try after few moment!!", Toast.LENGTH_SHORT).show()
            Snackbar.make(result_main, "Dependencies are not loaded yet...please try after few moment!!", Snackbar.LENGTH_SHORT).show()
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
        val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm:ss")
        val currentDate = sdf.format(Date())

        // Create JSON entry
        val entry = JSONObject()
        entry.put("Uri", uri)
        entry.put("Text", text)
        entry.put("Date", currentDate)
        json.put(entry)
        prefEditor.putString("history", json.toString())
        prefEditor.apply() // handle writing in the background
    }

    private val runnable: Runnable = object : Runnable {
        override fun run() {
            // The method you want to call every now and then.
            handleSelection()
            handler.postDelayed(this, 300) // This time is in millis.
        }
    }

    private fun handleSelection() {
        val res = myDrawView.calculateBoundingBox()
        Log.d("DRAWING", res.toString())

        if(res.size == 0) return
        if(res[0] == 0.0 && res[1] == 0.0 && res[2] == 0.0 && res[3] == 0.0) return
        image_view.scaleX
        image_view.scaleY

        val drawable: BitmapDrawable? = image_view.drawable as BitmapDrawable?

        val cropped: Bitmap = Bitmap.createBitmap(
            drawable!!.bitmap,
            (res[0]*image_view.scaleX).toInt(),
            (res[1]*image_view.scaleY).toInt(),
            (res[2]*image_view.scaleX).toInt(),
            (res[3]*image_view.scaleY).toInt()
        )

        image_view.setImageBitmap(cropped)

        val imFrame = Frame.Builder().setBitmap(cropped).build()
        val textBlocks = textRecognizer.detect(imFrame)

        val stringBuilder = StringBuilder()
        for (i in 0 until textBlocks.size()) {
            val textBlock = textBlocks[textBlocks.keyAt(i)]
            stringBuilder.append(textBlock.value)
            stringBuilder.append("\n")
        }
        ocrResult = stringBuilder.toString()
    }

}

class DoAsync(val handler: () -> Unit) : AsyncTask<Void, Void, Void>() {
    init {
        execute()
    }

    override fun doInBackground(vararg params: Void?): Void? {
        handler()
        return null
    }
}
