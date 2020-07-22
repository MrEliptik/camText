package com.victormeunier.camtext

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_history.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File


class HistoryActivity : AppCompatActivity() {

    private var selectId: Int = 0
    private lateinit var listItems: JSONArray
    private lateinit var adapter: HistoryAdapter
    private var mOptionsMenu: Menu? = null
    private val RESULT_CODE = 3
    private var modal_showing = false
    private var selecting = false
    private var selected = ArrayList<Int>()
    private var deleteId: Int = 0
    private var allSelected: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val items = getHistoryItems()
        if(items.length() > 0){ displayHistoryItems(items) }

        clear_button.setOnClickListener {
            AlertDialog.Builder(this)
                .setIcon(R.drawable.alert_24)
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
        if (selecting) {
            selecting = false
            title = resources.getString(R.string.history_activity);
            for (item in selected){
                history_list_view.setItemChecked(item, false)
            }
            selected.clear()
            mOptionsMenu?.removeItem(0)
            mOptionsMenu?.removeItem(1)
        }
        else finish()
        return true
    }

    override fun onBackPressed() {
        if (selecting) {
            selecting = false
            title = resources.getString(R.string.history_activity);
            for (item in selected){
                history_list_view.setItemChecked(item, false)
            }
            selected.clear()
            mOptionsMenu?.removeItem(0)
            mOptionsMenu?.removeItem(1)
        }
        else super.onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        mOptionsMenu = menu;
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
                    .setIcon(R.drawable.alert_24)
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
            deleteId -> {
                Log.d("MENU", "DELETE")
                AlertDialog.Builder(this)
                    .setIcon(R.drawable.alert_24)
                    .setTitle(resources.getString(R.string.erase_history))
                    .setMessage(resources.getString(R.string.msg_erase_history))
                    .setPositiveButton(resources.getString(R.string.yes)
                    ) { dialog, which ->
                        run {
                            clearHistory(selected)
                            selecting = false
                            allSelected = false
                            title = resources.getString(R.string.history_activity);
                            for (item in selected){
                                history_list_view.setItemChecked(item, false)
                            }
                            selected.clear()
                            mOptionsMenu?.removeItem(0)
                            mOptionsMenu?.removeItem(1)
                        }}
                    .setNegativeButton(resources.getString(R.string.no), null)
                    .show()
                true

            }
            selectId -> {
                Log.d("MENU", "SELECT")
                if (allSelected){
                    for (item in 0 until listItems.length()){
                        history_list_view.setItemChecked(item, false)
                    }
                    allSelected = false
                    selected.clear()
                    mOptionsMenu?.findItem(0)?.setIcon(R.drawable.select_all_24)
                    title = selected.size.toString() +" "+ resources.getString(R.string.selecting)
                }
                else {
                    for (item in 0 until listItems.length()){
                        if (item in selected) continue
                        history_list_view.setItemChecked(item, true)
                        selected.add(item)
                    }
                    allSelected = true
                    mOptionsMenu?.findItem(0)?.setIcon(R.drawable.unselect_all_24)
                    title = selected.size.toString() +" "+ resources.getString(R.string.selecting)
                }
                true
            }
            else -> false
        }
    }

    private fun rateMyApp() {
        val uri: Uri = Uri.parse("market://details?id=" + applicationContext.packageName)
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
                    Uri.parse("http://play.google.com/store/apps/details?id=" + applicationContext.packageName)
                )
            )
        }
    }

    private fun clearHistory(selected: ArrayList<Int>? = null) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this /* Activity context */)
        if (selected != null) {
            // get the items as JSONArray
            val arr = getHistoryItems()
            var size = arr.length()
            var newArr = JSONArray()

            // Delete selected items
            // Delete selected items (careful: position is in reverse compared
            // to the JSONArray)
            for (i in (size-1) downTo 0) {
                val item = arr.get(i) as JSONObject
                if ((size-1)-i !in selected){
                    newArr.put(item)
                }
                // Delete image if needed
                else {
                    if (sharedPreferences.getBoolean("image_history", true)) {
                        val uri = Uri.parse(item.get("Uri") as String?)
                        val file = File(uri.path)
                        file?.delete()
                    }
                }
                //arr.remove((size-1) - i)
            }

            // Put back in sharedpreference
            val sharedPref = getSharedPreferences("appData", Context.MODE_PRIVATE)
            val prefEditor = sharedPref.edit()
            prefEditor.putString("history", newArr.toString())
            prefEditor.apply() // handle writing in the background


            var newListItems = JSONArray()
            size = listItems.length()
            // Update data

            for (i in 0 until size) {
                val item = listItems.get(i)
                if (i !in selected) {
                    newListItems.put(item)
                }
            }

            listItems = JSONArray(newListItems.toString())
            adapter = HistoryAdapter(this, listItems)
            history_list_view.adapter = adapter
            adapter.notifyDataSetChanged()
        }
        else {
            history_list_view.adapter = null
            val sharedPref = getSharedPreferences("appData", Context.MODE_PRIVATE)
            var editPref = sharedPref.edit()

            val arr = getHistoryItems()
            var size = arr.length()

            // Delete selected items
            // Delete selected items (careful: position is in reverse compared
            // to the JSONArray)
            if (sharedPreferences.getBoolean("image_history", true)) {
                for (i in 0 until size) {
                    val item = arr.get(i) as JSONObject
                    // Remove image if needed
                    val file = File(item.get("Uri").toString())
                    file?.delete()
                }
            }
            editPref.remove("history")
            editPref.apply()
        }
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
        listItems = JSONArray()
        for (i in items.length()-1 downTo 0) {
            val item: JSONObject = items.getJSONObject(i)
            listItems.put(item)
        }

        adapter = HistoryAdapter(this, listItems)
        history_list_view.adapter = adapter

        // Short press
        history_list_view.setOnItemClickListener { parent, view, position, id ->
            if(selecting) {
                if (position in selected){
                    selected.remove(position)
                    title = selected.size.toString() +" "+ resources.getString(R.string.selecting)
                    history_list_view.setItemChecked(position, false)
                    if (selected.size == 0) {
                        mOptionsMenu?.removeItem(1)
                        mOptionsMenu?.removeItem(0)
                        selecting = false
                        title = resources.getString(R.string.history_activity);
                    }
                }
                else{
                    selected.add(position)
                    title = selected.size.toString() +" "+ resources.getString(R.string.selecting)
                    history_list_view.setItemChecked(position, true)
                }
            }
            else {
                val item: JSONObject = adapter.getItem(position) as JSONObject // The item that was clicked

                val text = item.get("Text").toString()

                // Switch to resultActivity
                val i = Intent(applicationContext, ResultActivity::class.java)
                i.putExtra("imageUri", item.get("Uri") as String?)
                i.putExtra("text", item.get("Text") as String?)
                startActivity(i)
                history_list_view.setItemChecked(position, false)
                true
            }
        }

        // Long press
        history_list_view.setOnItemLongClickListener { parent, view, position, id ->
            Log.d("HISTORY", "Long press")
            if (selecting) {
                selecting = false
                title = resources.getString(R.string.history_activity);
                for (item in selected){
                    history_list_view.setItemChecked(item, false)
                }
                selected.clear()
                mOptionsMenu?.removeItem(1)
                mOptionsMenu?.removeItem(0)
            }
            else {
                selecting = true
                selected.add(position)
                title = selected.size.toString() +" "+ resources.getString(R.string.selecting);
                //view.setBackgroundColor(Color.parseColor("#8002AAF6"));
                history_list_view.setItemChecked(position, true)
                mOptionsMenu?.add(0, 0, 0, "Select all")?.setIcon(R.drawable.select_all_24)
                    ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                selectId = mOptionsMenu?.findItem(0)?.itemId!!
                mOptionsMenu?.add(0, 1, 1, "Delete")?.setIcon(R.drawable.bin_24)
                    ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                deleteId = mOptionsMenu?.findItem(1)?.itemId!!
            }

            /*
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
            */

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
