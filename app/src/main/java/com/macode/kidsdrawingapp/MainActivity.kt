package com.macode.kidsdrawingapp

import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.macode.kidsdrawingapp.databinding.ActivityMainBinding
import com.macode.kidsdrawingapp.databinding.DialogBrushSizeBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var brushSizeBinding: DialogBrushSizeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.drawingView.setSizeForBrush(10.toFloat())

        binding.brush.setOnClickListener {
            showBrushSizeChooserDialog()
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
}