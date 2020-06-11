package com.victormeunier.camtext

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import com.squareup.picasso.Picasso

class HistoryAdapter(private val context: Context,
                     private val dataSource: JSONArray
) : BaseAdapter() {

    private val inflater: LayoutInflater
            = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        // Get view for row item
        val rowView = inflater.inflate(R.layout.list_item_history, parent, false)

        // Get title element
        val titleTextView = rowView.findViewById(R.id.history_list_title) as TextView

        // Get subtitle element
        val dateTextView = rowView.findViewById(R.id.history_list_date) as TextView

        // Get thumbnail element
        val thumbnailImageView = rowView.findViewById(R.id.history_list_thumbnail) as ImageView

        val item = getItem(position) as JSONObject

        val text = item.get("Text").toString()
        titleTextView.text = text.substring(text.length.coerceAtLeast(50) - 50);
        dateTextView.text = item.get("Date").toString()


        Picasso.with(context).load(Uri.parse(item.get("Uri") as String?))
            .noFade()
            .resize(100, 100)
            .centerCrop()
            .placeholder(R.drawable.image_placeholder)
            .into(thumbnailImageView)

        return rowView
    }

    override fun getItem(position: Int): Any {
        return dataSource[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return dataSource.length()
    }
}