package com.example.web_socket_client

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.web_socket_client.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import org.tensorflow.lite.task.vision.classifier.Classifications
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.math.min

class MainActivity : AppCompatActivity(), ImageClassifierHelper.ClassifierListener {
    
    companion object {
        private const val NORMAL_CLOSURE_STATUS = 1000
    }
    
    private lateinit var imageClassifierHelper: ImageClassifierHelper
    private lateinit var binding: ActivityMainBinding
    private lateinit var client: OkHttpClient
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        
        setContentView(binding.root)
        client = OkHttpClient.Builder()
            .readTimeout(1, TimeUnit.DAYS)
            .build()
        
        binding.start.setOnClickListener {
            start()
        }


        imageClassifierHelper = ImageClassifierHelper(context = this, imageClassifierListener = this)
    }
    
    private fun start() {
        val request: Request = Request.Builder().url("ws://" + "192.168.234.134" + ":86/").build()
        
        val webSocketListener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("Websocket", "Open " + response.message)
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
            
            }
            
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                
                lifecycleScope.launch(Dispatchers.Main) {
                    Log.d("Websocket", "Receive");
                    val message = bytes.asByteBuffer()
                    val imageBytes = ByteArray(message.remaining())
                    message.get(imageBytes)
                    val bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: return@launch
                    val viewWidth: Int = binding.imageView.width
                    val matrix = Matrix()
//                    matrix.postRotate(90f)
                    val bmp_traspose = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                    val imagRatio = bmp_traspose.height.toFloat() / bmp_traspose.width.toFloat()
                    val dispViewH = (viewWidth * imagRatio).toInt()
                    val bitmap = Bitmap.createScaledBitmap(bmp_traspose, viewWidth, dispViewH, false)
                    binding.imageView.setImageBitmap(bitmap)
                    
                    imageClassifierHelper.classify(bitmap, Surface.ROTATION_0)
                }
                
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(NORMAL_CLOSURE_STATUS, null)
                Log.d("Websocket", "Closed reason: $reason code: $code")
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                webSocket.close(NORMAL_CLOSURE_STATUS, null)
                Log.d("Websocket", "Error " + t.message)
            }
            
            
        }
        val ws: WebSocket = client.newWebSocket(request, webSocketListener)
//        client.dispatcher.executorService.shutdown()
    }
    
    override fun onError(error: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onResults(results: List<Classifications>?, inferenceTime: Long) {
        updateResults(results)
    }
    
    private fun updateResults(listClassifications: List<Classifications>?) {
        listClassifications?.let { it ->
            if (it.isNotEmpty()) {
                val sortedCategories = it[0].categories.sortedBy { it?.index }
                binding.blindspot.text = when(sortedCategories.first().label){
                    "0" -> "Right"
                    "1" -> "Left"
                    "2" -> "Clear"
                    else -> "Unknown"
                }
                binding.confidence.text = String.format("%.2f", sortedCategories.first().score)
            }
        }
    }
    
    
}