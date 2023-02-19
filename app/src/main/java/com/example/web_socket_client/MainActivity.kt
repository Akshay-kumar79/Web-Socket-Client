package com.example.web_socket_client

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.web_socket_client.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okio.ByteString
import okio.ByteString.Companion.decodeHex

class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val NORMAL_CLOSURE_STATUS = 1000
    }
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var client: OkHttpClient
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        
        setContentView(binding.root)
        client = OkHttpClient()
        
        binding.start.setOnClickListener {
            start()
        }
    }
    
    private fun start() {
//        val request: Request = Request.Builder().url("ws://" + "192.168.151.134" + ":86/").build()   // realme
        val request: Request = Request.Builder().url("ws://" + "192.168.29.134" + ":86/").build()   // pixel
        
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
                    matrix.postRotate(90f)
                    val bmp_traspose = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                    val imagRatio = bmp_traspose.height.toFloat() / bmp_traspose.width.toFloat()
                    val dispViewH = (viewWidth * imagRatio).toInt()
                    binding.imageView.setImageBitmap(Bitmap.createScaledBitmap(bmp_traspose, viewWidth, dispViewH, false))
                }
                
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(NORMAL_CLOSURE_STATUS, null)
                Log.d("Websocket", "Closed reason: $reason code: $code")
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.d("Websocket", "Error " + t.message)
            }
            
            
        }
        val ws: WebSocket = client.newWebSocket(request, webSocketListener)
        client.dispatcher.executorService.shutdown()
    }
    
}