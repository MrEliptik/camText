package com.example.camtext

import android.content.Context
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_history.*
import org.json.JSONArray
import org.json.JSONObject

class HistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val items = getHistoryItems()
        if(items.length() > 0){ displayHistoryItems(items) }

        clear_button.setOnClickListener { clearHistory() }
        history_list_view.emptyView = empty_element
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
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
        val listItems = arrayOfNulls<String>(items.length())
        for (i in 0 until items.length()) {
            val item: JSONObject = items.getJSONObject(i)
            listItems[i] = item.get("Text") as String?
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listItems)
        history_list_view.adapter = adapter
    }
}
