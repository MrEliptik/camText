package com.victormeunier.camtext

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaActionSound
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.AspectRatio.RATIO_16_9
import androidx.camera.core.AspectRatio.RATIO_4_3
import androidx.camera.core.ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.FocusMeteringAction
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {

    private var backButtonCount: Int = 0
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    enum class FLASH_MODE {
        ON, OFF, AUTO
    }

    private var flash_state = FLASH_MODE.OFF
    private val REQUEST_IMAGE_ALBUM_ACCESS = 2
    private val REQUEST_SELECT_IMAGE_IN_ALBUM = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide();

        //set content view AFTER ABOVE sequence (to avoid crash)
        setContentView(R.layout.activity_main)

        // Request camera permission
        if (allPermissionsGranted()) {
            viewFinder.post {
                startCamera()
                setUpTapToFocus()
            }
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Request external storage permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_IMAGE_ALBUM_ACCESS)
        }

        // Setup the listener for take photo button
        camera_capture_button.setOnClickListener { takePhoto() }

        // Option menu
        option_button.setOnClickListener(View.OnClickListener { view ->
            showOptions(view)
        })

        // Exit btn
        exit_button.setOnClickListener {
            AlertDialog.Builder(this)
                .setMessage(resources.getString(R.string.sure_exit))
                .setCancelable(false)
                .setPositiveButton(resources.getString(R.string.yes)
                ) { dialog, id -> finish() }
                .setNegativeButton(resources.getString(R.string.no), null)
                .show()
        }

        // Gallery button
        gallery_button.setOnClickListener {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_SELECT_IMAGE_IN_ALBUM)
            }
            else{
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "image/*"
                if (intent.resolveActivity(packageManager) != null) {
                    startActivityForResult(intent, REQUEST_SELECT_IMAGE_IN_ALBUM)
                }
            }
        }

        // History button
        history_button.setOnClickListener {
            val i = Intent(applicationContext, HistoryActivity::class.java)
            startActivity(i)
        }

        flash_button.setOnClickListener {
            if (flash_state == FLASH_MODE.OFF) {
                flash_state = FLASH_MODE.ON
                flash_button.setImageResource(R.drawable.flash_active_48)
                flash_button.invalidate()
                if (camera!!.cameraInfo.hasFlashUnit()) {
                    camera!!.cameraControl.enableTorch(true)
                }
            }
            else if(flash_state == FLASH_MODE.ON){
                flash_state = FLASH_MODE.OFF
                flash_button.setImageResource(R.drawable.flash_deactived_48)
                flash_button.invalidate()
                if (camera!!.cameraInfo.hasFlashUnit()) {
                    camera!!.cameraControl.enableTorch(false)
                }
            }
            // TODO: re-implement with AUTO mode
            else {
                flash_state = FLASH_MODE.AUTO
                flash_button.setImageResource(R.drawable.flash_auto_48)
                flash_button.invalidate()
            }
        }

        outputDirectory = getOutputDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()

        main.setOnTouchListener(object : OnSwipeTouchListener(this@MainActivity) {
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                val myIntent = Intent(applicationContext, HistoryActivity::class.java)
                startActivityForResult(myIntent, 0)
            }
        })

    }

    override fun onResume() {
        super.onResume()

        // Request camera permission
        if (allPermissionsGranted()) {
            viewFinder.post {
                startCamera()
                setUpTapToFocus()
            }
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        blur.visibility = View.INVISIBLE
        loading_anim.visibility = View.GONE
        steady.visibility = View.INVISIBLE
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            takePhoto()
            return true
        }
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.option_menu, menu)
        return true
    }

    private fun showOptions(v: View) {
        PopupMenu(this, v).apply {
            setOnMenuItemClickListener(object: PopupMenu.OnMenuItemClickListener {
                override fun onMenuItemClick(item: MenuItem?): Boolean {
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

            })
            inflate(R.menu.option_menu)
            show()
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

    override fun onBackPressed() {
        if (backButtonCount >= 1) {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_HOME)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        } else {
            Toast.makeText(this, resources.getString(R.string.press_again_exit), Toast.LENGTH_SHORT).show()
            backButtonCount++
        }
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = kotlin.math.max(width, height).toDouble() / kotlin.math.min(width, height)
        if (kotlin.math.abs(previewRatio - RATIO_4_3_VALUE) <= kotlin.math.abs(previewRatio - RATIO_16_9_VALUE)) {
            return RATIO_4_3
        }
        return RATIO_16_9
    }

    private fun setUpTapToFocus() {
        viewFinder.setOnTouchListener { _, event ->
            Log.d("CAMERA", event.toString())
            if (event.action == MotionEvent.ACTION_DOWN) {
                val factory = SurfaceOrientedMeteringPointFactory(viewFinder.width.toFloat(), viewFinder.height.toFloat())
                val point = factory.createPoint(event.x, event.y)
                val action =  FocusMeteringAction.Builder(point).build()
                val cameraControl = camera?.cameraControl
                cameraControl?.startFocusAndMetering(action)
                return@setOnTouchListener true
            }
            else return@setOnTouchListener false
        }
    }

    private fun startCamera() {
        val metrics = DisplayMetrics().also {
            viewFinder.display.getRealMetrics(it)
        }
        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            preview = Preview.Builder()
                .setTargetAspectRatio(screenAspectRatio)
                .build()

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetAspectRatio(screenAspectRatio)
                .build()

            // Select back camera
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
                preview?.setSurfaceProvider(viewFinder.createSurfaceProvider(camera?.cameraInfo))

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        // Disable button to prevent multiple takes
        camera_capture_button.isEnabled = false

        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create timestamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg")

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        val sound = MediaActionSound()
        sound.play(MediaActionSound.SHUTTER_CLICK)

        //shutterEffect.visibility = View.VISIBLE

        // You can unbind from any UseCase
        CameraX.unbind(preview);
        // In this way TextureView will hold the last frame

        blur.visibility = View.VISIBLE
        loading_anim.visibility = View.VISIBLE
        steady.visibility = View.VISIBLE

        val handler = Handler()
        val runnable = Runnable { shutterEffect.visibility = View.INVISIBLE }
        //handler.postDelayed(runnable, 150)

        // Setup image capture listener which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // Disable button
                    camera_capture_button.isEnabled = true


                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    //Toast.makeText(baseContext, "Photo captured.", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)

                    // Switch to resultActivity
                    val i = Intent(applicationContext, ResultActivity::class.java)
                    i.putExtra("imageUri", savedUri.toString())
                    startActivity(i)
                }
            })
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() } }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    resources.getString(R.string.permission_not_granted),
                    Toast.LENGTH_SHORT).show()
                ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
            }
        }
        else if (requestCode == REQUEST_IMAGE_ALBUM_ACCESS) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,
                    resources.getString(R.string.permission_not_granted),
                    Toast.LENGTH_SHORT).show()
            }
        }
        // Access with intent to open the gallery right after
        else if (requestCode == REQUEST_SELECT_IMAGE_IN_ALBUM) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,
                    resources.getString(R.string.permission_not_granted),
                    Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "image/*"
                if (intent.resolveActivity(packageManager) != null) {
                    startActivityForResult(intent, REQUEST_SELECT_IMAGE_IN_ALBUM)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_SELECT_IMAGE_IN_ALBUM){
            // Switch to resultActivity
            val i = Intent(applicationContext, ResultActivity::class.java)
            i.putExtra("imageUri", data?.data.toString())
            startActivity(i)
        }
    }

    companion object {
        private const val TAG = "CamText"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}





