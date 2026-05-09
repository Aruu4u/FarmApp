package com.example.farmapp.ui.view


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

import com.example.farmapp.R

import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import org.tensorflow.lite.Interpreter

import java.nio.channels.FileChannel

class DiseaseDetectionActivity : AppCompatActivity() {

    private lateinit var ivPreview: ImageView
    private lateinit var tvResult: TextView
    private lateinit var interpreter: Interpreter
    private lateinit var labels: List<String>

    private val IMAGE_SIZE = 224  // model input size
    private val PICK_IMAGE = 100
    private var selectedBitmap: Bitmap? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_disease_detection)

        ivPreview = findViewById(R.id.ivPreview)
        tvResult = findViewById(R.id.tvResult)
        val btnPick = findViewById<Button>(R.id.btnPickImage)
        val btnDetect = findViewById<Button>(R.id.btnDetect)

        // Load model + labels
        interpreter = Interpreter(loadModelFile("model_unquant.tflite"))
        labels = assets.open("labels.txt").bufferedReader().readLines()

        btnPick.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE)
        }

        btnDetect.setOnClickListener {
            selectedBitmap?.let {
                val resized = Bitmap.createScaledBitmap(it, IMAGE_SIZE, IMAGE_SIZE, true)
                val result = classifyImage(resized)
                tvResult.text = " Disease: ${labels[result]}"
            } ?: run {
                tvResult.text = "Please select an image first"
            }
        }
    }

    private fun loadModelFile(modelName: String): MappedByteBuffer {
        val fileDescriptor = assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun classifyImage(bitmap: Bitmap): Int {
        val input = Array(1) { Array(IMAGE_SIZE) { Array(IMAGE_SIZE) { FloatArray(3) } } }
        for (x in 0 until IMAGE_SIZE) {
            for (y in 0 until IMAGE_SIZE) {
                val pixel = bitmap.getPixel(x, y)
                input[0][x][y][0] = ((pixel shr 16) and 0xFF) / 255f
                input[0][x][y][1] = ((pixel shr 8) and 0xFF) / 255f
                input[0][x][y][2] = (pixel and 0xFF) / 255f
            }
        }

        val output = Array(1) { FloatArray(labels.size) }
        interpreter.run(input, output)

        return output[0].indices.maxByOrNull { output[0][it] } ?: -1
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = data?.data
            imageUri?.let {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
                ivPreview.setImageBitmap(bitmap)
                selectedBitmap = bitmap
            }
        }
    }
}
