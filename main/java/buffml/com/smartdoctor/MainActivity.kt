package buffml.com.smartdoctor

//package buffml.com.smartdoctor

import android.os.Bundle
import android.widget.Toast
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var mClassifier: Classifier
    private lateinit var mBitmap: Bitmap

    private val mInputSize = 224
    private val mModelPath = "model.tflite"
    private val mLabelPath = "labels.txt"
    private val mSamplePath = "skin-icon.jpg"  // Change .jpg to .jpeg


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_main)

        mClassifier = Classifier(assets, mModelPath, mLabelPath, mInputSize)

        resources.assets.open(mSamplePath).use {
            mBitmap = BitmapFactory.decodeStream(it)
            mBitmap = Bitmap.createScaledBitmap(mBitmap, mInputSize, mInputSize, true)
            findViewById<ImageView>(R.id.mPhotoImageView).setImageBitmap(mBitmap)
        }

        val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val photo = result.data!!.extras?.get("data") as Bitmap
                mBitmap = scaleImage(photo)
                Toast.makeText(this, "Image cropped: ${mBitmap.width}x${mBitmap.height}", Toast.LENGTH_LONG).show()
                findViewById<ImageView>(R.id.mPhotoImageView).setImageBitmap(mBitmap)
                findViewById<TextView>(R.id.mResultTextView).text = "Your photo is set."
            } else {
                Toast.makeText(this, "Camera cancelled", Toast.LENGTH_LONG).show()
            }
        }

        val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                try {
                    mBitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    mBitmap = scaleImage(mBitmap)
                    findViewById<ImageView>(R.id.mPhotoImageView).setImageBitmap(mBitmap)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        findViewById<Button>(R.id.mCameraButton).setOnClickListener {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraLauncher.launch(cameraIntent)
        }

        findViewById<Button>(R.id.mGalleryButton).setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        findViewById<Button>(R.id.mDetectButton).setOnClickListener {
            val results = mClassifier.recognizeImage(mBitmap).firstOrNull()
            findViewById<TextView>(R.id.mResultTextView).text =
                results?.title + "\nConfidence: " + results?.confidence
        }
    }

    private fun scaleImage(bitmap: Bitmap?): Bitmap {
        val originalWidth = bitmap!!.width
        val originalHeight = bitmap.height
        val scaleWidth = mInputSize.toFloat() / originalWidth
        val scaleHeight = mInputSize.toFloat() / originalHeight
        val matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight)
        return Bitmap.createBitmap(bitmap, 0, 0, originalWidth, originalHeight, matrix, true)
    }
}
