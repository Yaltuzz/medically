package com.example.medically

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.extensions.OrtxPackage
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import java.nio.FloatBuffer
import java.nio.IntBuffer
import android.Manifest
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : FlutterActivity() {
    private val CHANNEL_NLP = "naturalLanguageProcessing"
    private val CHANNEL_ASR = "automaticSpeechRecognition"
    private val stopRecordingFlag = AtomicBoolean(false)
    private val workerThreadExecutor = Executors.newSingleThreadExecutor()
    private var modelFilePath = ""
    private fun hasRecordAudioPermission(): Boolean =
        ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION_REQUEST_CODE) {
            if (!hasRecordAudioPermission()) {
                Toast.makeText(
                    this,
                    "Permission to record audio was not granted.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun onStartRecording(){
        if (!hasRecordAudioPermission()) {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_PERMISSION_REQUEST_CODE
            )
            return onStartRecording()
        }
        workerThreadExecutor.submit {
            try {
                stopRecordingFlag.set(false)
                val audioTensor = AudioTensorSource.fromRecording(stopRecordingFlag)
                    val environment = OrtEnvironment.getEnvironment()
                    val sessionOptions = OrtSession.SessionOptions()
                    sessionOptions.registerCustomOpLibrary(OrtxPackage.getLibraryPath())
                    val session: OrtSession = environment.createSession(modelFilePath, sessionOptions)
                    val forcedDecoderIds = intArrayOf(50258, 50269, 50359, 50363)
                    val tensorShape =
                        longArrayOf(1, forcedDecoderIds.size.toLong())
                    val tensorBuffer = IntBuffer.wrap(forcedDecoderIds)
                    val onnxTensor = OnnxTensor.createTensor(environment, tensorBuffer, tensorShape)
                    val baseInputs = mapOf(
                        "min_length" to createIntTensor(environment, intArrayOf(1), tensorShape(1)),
                        "max_length" to createIntTensor(
                            environment,
                            intArrayOf(200),
                            tensorShape(1)
                        ),
                        "num_beams" to createIntTensor(environment, intArrayOf(1), tensorShape(1)),
                        "num_return_sequences" to createIntTensor(
                            environment,
                            intArrayOf(1),
                            tensorShape(1)
                        ),
                        "decoder_input_ids" to onnxTensor,
                        "length_penalty" to createFloatTensor(
                            environment,
                            floatArrayOf(1.0f),
                            tensorShape(1)
                        ),
                        "repetition_penalty" to createFloatTensor(
                            environment,
                            floatArrayOf(1.0f),
                            tensorShape(1)
                        ),
                    )

                    val inputs = mutableMapOf<String, OnnxTensor>()
                    baseInputs.toMap(inputs)
                    inputs["audio_pcm"] = audioTensor
                    val outputs = session.run(inputs)
                    val recognizedText = outputs.use {
                        @Suppress("UNCHECKED_CAST")
                        (outputs[0].value as Array<Array<String>>)[0][0]
                    }
                    runOnUiThread {
                        MethodChannel(flutterEngine!!.dartExecutor.binaryMessenger, "flutter_channel").invokeMethod("flutterMethod", recognizedText)
                    }
                session.close()
                environment.close()
               // }
            } catch (e: Exception) {
                println(e)
            }
        }
    }

    fun copyModelToInternalStorage(context: Context): String {
        val modelInputStream = context.resources.openRawResource(R.raw.model_whisper)
        val modelFile = File(context.filesDir, "model_whisper.onnx")

        modelInputStream.use { input ->
            FileOutputStream(modelFile).use { output ->
                val buffer = ByteArray(1024)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
            }
        }
        return modelFile.absolutePath
    }

    fun onStopRecording() {
        stopRecordingFlag.set(true)
    }

    companion object {
        const val TAG = "ORTSpeechRecognizer"
        private const val RECORD_AUDIO_PERMISSION_REQUEST_CODE = 1
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
        val py = Python.getInstance()
        val pyObj = py.getModule("myScript")
        modelFilePath = copyModelToInternalStorage(context)


        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            CHANNEL_NLP
        ).setMethodCallHandler { call, result ->
            if (call.method == "answer") {
                val question: String? = call.argument("question")
                val context: String? = call.argument("context")
                val message = pyObj.callAttr("predict", question, context)
                println(message.toString())
                result.success(message.toString())
            } else {
                result.notImplemented()
            }
        }

        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            CHANNEL_ASR
        ).setMethodCallHandler { call, result ->
            if (call.method == "start_recording") {
                onStartRecording()
                result.success("okay")
            } else if (call.method == "stop_recording") {
                onStopRecording()
                result.success("okay")
            } else {
                result.notImplemented()
            }
        }
    }
}

internal fun createIntTensor(env: OrtEnvironment, data: IntArray, shape: LongArray): OnnxTensor {
    return OnnxTensor.createTensor(env, IntBuffer.wrap(data), shape)
}

internal fun createFloatTensor(
    env: OrtEnvironment,
    data: FloatArray,
    shape: LongArray
): OnnxTensor {
    return OnnxTensor.createTensor(env, FloatBuffer.wrap(data), shape)
}

internal fun tensorShape(vararg dims: Long) = longArrayOf(*dims)
