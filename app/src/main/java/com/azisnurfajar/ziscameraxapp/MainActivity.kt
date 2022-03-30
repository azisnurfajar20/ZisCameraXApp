package com.azisnurfajar.ziscameraxapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import androidx.camera.core.*
import com.azisnurfajar.ziscameraxapp.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.camera.lifecycle.ProcessCameraProvider
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*

typealias LumaListener = (luma: Double) -> Unit

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // TODO 1(203310027 Azis Nur Fajar) : Meminta Izin untuk mengaktifkan Kamera
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
        //TODO 2 (203310027 Azis Nur Fajar) : Menyiapkan untuk mengambil foto dan tombol pengambilan video
        viewBinding.imageCaptureButton.setOnClickListener { takePhoto() }
        cameraExecutor = Executors.newSingleThreadExecutor()
    }
    private fun takePhoto() {
        // TODO 3 (203310027 Azis Nur Fajar) : Mengambil gambar
        val imageCapture = imageCapture ?: return
        // TODO 4 (203310027 Azis Nur Fajar) : Untuk menambahkan Nama pada File
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            // TODO 5 (203310027 Azis Nur Fajar) : Untuk menentukan format file pada gambar
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            // TODO 6 (203310027 Azis Nur Fajar) : Untuk Menyimpan Foto dengan nama Folder ZisCameraXApp
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ZisCameraXApp")
            }
        }
        // TODO 7 (203310027 Azis Nur Fajar) : Untuk membuat objek opsi keluaran yang berisi file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()
        // TODO 8 (203310027 Azis Nur Fajar) : Siapkan pendengar pengambilan gambar, yang dipicu setelah foto diambil
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }
                override fun
                        onImageSaved(output: ImageCapture.OutputFileResults){
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            }
        )
    }

    private class LuminosityAnalyzer(private val listener: LumaListener) : ImageAnalysis.Analyzer {
        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Mengulang buffer
            val data = ByteArray(remaining())
            get(data)
            return data
        }
        override fun analyze(image: ImageProxy) {
            val rotation = image.imageInfo.rotationDegrees
            val buffer = image.planes[0].buffer
            val data = buffer.toByteArray()
            val pixels = data.map { it.toInt() and 0xFF }
            val luma = pixels.average()
            listener(luma)
            image.close()
        }
    }
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            // TODO 9 (203310027 Azis Nur Fajar) : Untuk menghidupkan kamera
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            // TODO 10 (203310027 Azis Nur Fajar) : Hasil
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }
            imageCapture = ImageCapture.Builder().build()
            // TODO 11 (203310027 Azis Nur Fajar) : Untuk Mengaktifkan kamera belakang sebagai kamera utama
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)
            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }
    // TODO 12 (203310027 Azis Nur Fajar) : Memberikan alert untuk izin aplikasi
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
    // TODO 13 (203310027 Azis Nur Fajar) : Memberikan fomrat nama file
    companion object {
        private const val TAG = "ZisCameraX"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}
