import android.app.Activity
import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.widget.Button
import com.victormeunier.camtext.R
import kotlinx.android.synthetic.main.modal_item_history.*

class HistoryDialogClass(var c: Activity, val uri: String, val text: String, val date: String) : Dialog(c), View.OnClickListener {
    var d: Dialog? = null
    var yes: Button? = null
    var no: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.modal_item_history)

        exit_btn.setOnClickListener {
            dismiss()
        }

        var imageUri = Uri.parse(uri)

        //set image captured to image view
        history_list_thumbnail.setImageURI(imageUri)

        history_list_text.text = text
        history_list_date.text = date

    }

    override fun onClick(v: View) {
        dismiss()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        Log.d("MODAL", event.toString())
        return false
    }

}