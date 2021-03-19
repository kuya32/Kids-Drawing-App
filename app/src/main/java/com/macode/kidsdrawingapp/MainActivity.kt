package com.macode.kidsdrawingapp

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import com.macode.kidsdrawingapp.databinding.ActivityMainBinding
import com.macode.kidsdrawingapp.databinding.DialogBrushSizeBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var brushSizeBinding: DialogBrushSizeBinding
    private var imageButtonCurrentPaint: ImageButton? = null
    private lateinit var progressDialog: Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.drawingView.setSizeForBrush(10.toFloat())

        imageButtonCurrentPaint = binding.linearColorPalette[0] as ImageButton
        imageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_selected)
        )

        binding.brush.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        binding.gallery.setOnClickListener {
            if (isReadStorageAllowed()) {
                val pickPhotoIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(pickPhotoIntent, GALLERY)
            } else {
                requestStoragePermission()
            }
        }

        binding.undo.setOnClickListener {
            binding.drawingView.onClickUndo()
        }

        binding.redo.setOnClickListener {
            binding.drawingView.onClickRedo()
        }

        binding.save.setOnClickListener {
            if (isReadStorageAllowed()) {

                CoroutineScope(Main).launch {
                    showProgressDialog()
                    saveImage(getBitmapFromView(binding.frameDrawingViewContainer))
                    cancelProgressDialog()
                }
            } else {
                requestStoragePermission()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GALLERY) {
                try {
                    if (data!!.data != null) {
                        binding.imageViewBackground.visibility = View.VISIBLE
                        binding.imageViewBackground.setImageURI(data.data)
                    } else {
                        Toast.makeText(this@MainActivity, "Error in parsing image or its corrupted.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun showBrushSizeChooserDialog() {
        brushSizeBinding = DialogBrushSizeBinding.inflate(layoutInflater)
        val view = brushSizeBinding.root
        val brushDialog = Dialog(this)
        brushDialog.setContentView(view)
        brushDialog.setTitle("Brush size: ")

        val extraSmallButton = brushSizeBinding.extraSmallBrush
        val smallButton = brushSizeBinding.smallBrush
        val mediumButton = brushSizeBinding.mediumBrush
        val largeButton = brushSizeBinding.largeBrush
        val extraLargeButton = brushSizeBinding.extraLargeBrush

        extraSmallButton.setOnClickListener {
            binding.drawingView.setSizeForBrush(5.toFloat())
            brushDialog.dismiss()
        }

        smallButton.setOnClickListener {
            binding.drawingView.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }

        mediumButton.setOnClickListener {
            binding.drawingView.setSizeForBrush(15.toFloat())
            brushDialog.dismiss()
        }

        largeButton.setOnClickListener {
            binding.drawingView.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }

        extraLargeButton.setOnClickListener {
            binding.drawingView.setSizeForBrush(25.toFloat())
            brushDialog.dismiss()
        }

        brushDialog.show()
    }

    fun paintClicked(view: View) {
        if (view != imageButtonCurrentPaint) {
            val imageButton = view as ImageButton

            val colorTag = imageButton.tag.toString()
            binding.drawingView.setColor(colorTag)
            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_selected)
            )
            imageButtonCurrentPaint!!.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_normal)
            )
            imageButtonCurrentPaint = view
        }
    }

    private fun requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE).toString())) {
            Toast.makeText(this, "Need permission to add a background", Toast.LENGTH_SHORT).show()
        }
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this@MainActivity, "Permission granted, now you can read the storage files.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "Permission denied!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isReadStorageAllowed(): Boolean {
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)

        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun getBitmapFromView(view: View) : Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val backgroundDrawable = view.background
        if (backgroundDrawable != null) {
            backgroundDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)

        return returnedBitmap
    }

    private suspend fun saveImage(image: Bitmap): String? {
        var savedImagePath: String? = null
        val imageFileName = "KidDrawingApp_" + System.currentTimeMillis() / 1000 + ".png"
        val storageDir = File(externalCacheDir!!.absoluteFile.toString() + File.separator + "KidDrawingApp_" + System.currentTimeMillis() / 1000 + ".png")
        var success = true
        if (!storageDir.exists()) {
            success = storageDir.mkdirs()
        }
        if (success) {
            val imageFile = File(storageDir, imageFileName)
            savedImagePath = imageFile.absolutePath
            try {
                val fOut: OutputStream = FileOutputStream(imageFile)
                image.compress(Bitmap.CompressFormat.PNG, 100, fOut)
                fOut.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            galleryAddImage(savedImagePath)

            Toast.makeText(this, "$imageFileName saved!", Toast.LENGTH_SHORT).show()
        }
        return savedImagePath
    }

    private fun galleryAddImage(imagePath: String?) {
        imagePath?.let { path ->
            val f = File(path)
            MediaScannerConnection.scanFile(this@MainActivity, arrayOf(f.toString()),
                null) { path, uri ->
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                shareIntent.type = "image/png"

                startActivity(Intent.createChooser(shareIntent, "Share"))
            }
        }
    }

    private suspend fun showProgressDialog() {
        progressDialog = Dialog(this@MainActivity)
        progressDialog.setContentView(R.layout.dialog_custom_progress)
        progressDialog.show()
    }

    private fun cancelProgressDialog() {
        progressDialog.dismiss()
    }

    companion object {
        private const val STORAGE_PERMISSION_CODE = 1
        private const val GALLERY = 2
    }
}