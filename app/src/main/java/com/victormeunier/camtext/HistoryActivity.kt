package com.victormeunier.camtext

import HistoryDialogClass
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_history.*
import org.json.JSONArray
import org.json.JSONObject


class HistoryActivity : AppCompatActivity() {

    private val RESULT_CODE = 3
    private var modal_showing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val items = getHistoryItems()
        if(items.length() > 0){ displayHistoryItems(items) }

        clear_button.setOnClickListener {
            AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Erasing history")
                .setMessage("Are you sure you want to erase history?")
                .setPositiveButton("Yes"
                ) { dialog, which ->
                    run {
                        clearHistory()
                    }}
                .setNegativeButton("No", null)
                .show()
        }
        history_list_view.emptyView = empty_element


        main_history.setOnTouchListener(object : OnSwipeTouchListener(this@HistoryActivity) {
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                val myIntent = Intent(applicationContext, MainActivity::class.java)
                startActivityForResult(myIntent, 0)
            }

            override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
                if (motionEvent.action == MotionEvent.ACTION_UP) {
                    if (modal_showing) {
                        modal_showing = false
                        Log.d("HISTORY", "Modal $modal_showing")
                    }
                }
                return super.onTouch(view, motionEvent)
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.history_option_menu, menu)
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
            R.id.action_erase -> {
                Log.d("MENU", "ERASE")
                AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(resources.getString(R.string.erase_history))
                    .setMessage(resources.getString(R.string.msg_erase_history))
                    .setPositiveButton(resources.getString(R.string.yes)
                    ) { dialog, which ->
                        run {
                            clearHistory()
                        }}
                    .setNegativeButton(resources.getString(R.string.no), null)
                    .show()
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

    private fun clearHistory() {
        history_list_view.adapter = null
        val sharedPref = getSharedPreferences("appData", Context.MODE_PRIVATE)
        var editPref = sharedPref.edit()
        editPref.remove("history")
        editPref.apply()
    }

    private fun getHistoryItems(): JSONArray {
        val sharedPref = getSharedPreferences("appData", Context.MODE_PRIVATE)
        var json = JSONArray()
        // Retrieve values from preferences
        val str: String? = sharedPref.getString("history", null)
        if(str != null) json = JSONArray(str)
        return json
    }

    private fun displayHistoryItems(items:JSONArray) {
        //val listItems = arrayOfNulls<String>(items.length())
        val listItems = JSONArray()
        for (i in items.length()-1 downTo 0) {
            val item: JSONObject = items.getJSONObject(i)
            //listItems[i] = item.get("Text") as String?
            listItems.put(item)
        }

        //val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listItems)
        val adapter = HistoryAdapter(this, listItems)
        history_list_view.adapter = adapter

        // Short press
        history_list_view.setOnItemClickListener { parent, view, position, id ->
            val item: JSONObject = adapter.getItem(position) as JSONObject // The item that was clicked

            val text = item.get("Text").toString()

            // Switch to resultActivity
            val i = Intent(applicationContext, ResultActivity::class.java)
            i.putExtra("imageUri", item.get("Uri") as String?)
            i.putExtra("text", item.get("Text") as String?)
            startActivity(i)
        }

        // Long press
        history_list_view.setOnItemLongClickListener { parent, view, position, id ->
            val element = adapter.getItem(position) // The item that was clicked
            Log.d("HISTORY", "Long press")

            val item: JSONObject = adapter.getItem(position) as JSONObject // The item that was clicked
            val text = item.get("Text").toString()

            val d = HistoryDialogClass(this, item.get("Uri") as String, text,
                item.get("Date") as String
            )
            d.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));

            val dm = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(dm)

            val w = dm.widthPixels
            val h = dm.heightPixels

            val window = d.getWindow();
            window?.setLayout((w*0.8).toInt(), (h*0.75).toInt())
            d.show()

            true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            Log.i("onActivityResult", data?.getStringExtra("result"))
        }
    }
}
