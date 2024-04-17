package com.example.medically

import android.content.Context
import io.flutter.embedding.android.FlutterActivity
import androidx.annotation.NonNull
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.nio.ByteBuffer
import java.nio.ByteOrder
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtLoggingLevel
import ai.onnxruntime.OrtSession
import ai.onnxruntime.extensions.OrtxPackage
import android.os.SystemClock
import java.util.Collections

class MainActivity: FlutterActivity() {
  private val CHANNEL_STT = "speechToText"
  private val CHANNEL_NLP = "naturalLanguageProcessing"

  override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
      super.configureFlutterEngine(flutterEngine)
      val recognizer = AudioRecognizer(this)
      val interpreter = TextInterpreter(this)
      MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL_STT).setMethodCallHandler(recognizer)
      MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL_NLP).setMethodCallHandler(interpreter)
  }
}

class TextInterpreter(private val context: Context): MethodChannel.MethodCallHandler{
    private val recognizeMethod = "interpret_text"

    private val textAnswering: TextAnswering by lazy {
        val resources = context.resources
        resources.openRawResource(R.raw.csarron_mobilebert_uncased_squad_v2_quant_with_pre_post_processing).use {
            val modelBytes = it.readBytes()
            TextAnswering(modelBytes)
        }
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            recognizeMethod -> interpretText(call, result)
            else -> result.notImplemented()
        }
    }
    private fun interpretText(call: MethodCall, result: MethodChannel.Result)
    {
        val bytes: ByteArray? =  call.argument("data")
        if (bytes==null) {
            result.error("Some arguments have null value", "Some arguments have null value.", null)
        }

        val text = "Michael Jordan is widely regarded as one of the greatest basketball players of all time. His extraordinary talent, unparalleled work ethic, and relentless competitiveness propelled him to legendary status in the world of sports. Jordan's impact extends far beyond the basketball court, as he transcended the game to become a global icon and cultural phenomenon. From his numerous NBA championships to his iconic brand, Jordan's legacy continues to inspire aspiring athletes and fans around the world."
        val question = "Who is Micheal Jordan?"
        val inputTensor = fromString(text,question)


        val results = inputTensor.use { textAnswering.run(inputTensor) }
        result.success(results.text)
    }

    fun fromString(text: String, question: String): OnnxTensor {
        val shape = longArrayOf(1, 2)
        val env = OrtEnvironment.getEnvironment()
        return OnnxTensor.createTensor(env, arrayOf(question, text), shape)
    }
}

class TextAnswering(modelBytes: ByteArray) : AutoCloseable {
    private var session: OrtSession
    private var Cur_EP:String = ""
    init {
        session = createOrtSession("CPU",modelBytes)
    }
    fun run(inputTensor: OnnxTensor): Result {
        var recognizedText: String = ""
        var elapsedTimeInMs: Long = 0
        inputTensor.use {
            // Step 3: call ort inferenceSession run
            val startTimeInMs = SystemClock.elapsedRealtime()
            val output = session.run(Collections.singletonMap("input_text", inputTensor))
            elapsedTimeInMs = SystemClock.elapsedRealtime() - startTimeInMs

            // Step 4: output analysis
            output.use {
                val rawOutput = (output?.get(0)?.value) as Array<String>

                // Step 5: set output result
                recognizedText = rawOutput[0]
            }
        }
        return Result(recognizedText, elapsedTimeInMs)
    }
    fun createOrtSession(ep:String, modelBytes: ByteArray):OrtSession {
        if (Cur_EP.equals(ep)){
            throw IllegalArgumentException("ep equals none")
        }
        Cur_EP = ep
        // Initialize Ort Session and register the onnxruntime extensions package that contains the custom operators.
        // Note: These are used to load tokenizer and post-processor ops
        val sessionOptions: OrtSession.SessionOptions = OrtSession.SessionOptions()
        if (ep.contains("CPU")){

        }else if (ep.contains("NNAPI")) {
            sessionOptions.addNnapi()
        }else if (ep.contains("XNNAPCK")) {
            val po = mapOf<String, String>()
            sessionOptions.addXnnpack(po)
        }
        val env = OrtEnvironment.getEnvironment()
        sessionOptions.setSessionLogLevel(OrtLoggingLevel.ORT_LOGGING_LEVEL_VERBOSE)
        sessionOptions.setSessionLogVerbosityLevel(0)
        sessionOptions.registerCustomOpLibrary(OrtxPackage.getLibraryPath())
        return env.createSession(modelBytes, sessionOptions)
    }
    override fun close() {
        session.close()
    }

}
class AudioRecognizer(private val context: Context): MethodChannel.MethodCallHandler {
  private val recognizeMethod = "recognize_audio"
    private  val bytesPerFloat = 4
    private  val sampleRate = 16000
    private  val maxAudioLengthInSeconds = 30

    private val speechRecognizer: SpeechRecognizer by lazy {
        val resources = context.resources
        resources.openRawResource(R.raw.whisper_cpu_int8_model).use {
            val modelBytes = it.readBytes()
            SpeechRecognizer(modelBytes)
        }
    }
  override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
      when(call.method) {
          recognizeMethod -> recognizeAudio(call, result)
          else -> result.notImplemented()
      }
  }

  private fun recognizeAudio(call: MethodCall, result: MethodChannel.Result) {
      val bytes: ByteArray? =  call.argument("data")
      if (bytes==null){
          result.error("Some arguments have null value", "Some arguments have null value.", null);
          return
      }
      val resources = context.resources
      val audioTensor = resources.openRawResource(R.raw.audio_mono_16khz_f32le).use {
          fromRawPcmBytes(it.readBytes())
      }
      //val audioTensor = fromRawPcmBytes(bytes)
      val results = audioTensor.use { speechRecognizer.run(audioTensor) }
      result.success(results.text)
  }

    fun fromRawPcmBytes(rawBytes: ByteArray): OnnxTensor {
        val rawByteBuffer = ByteBuffer.wrap(rawBytes)
        // TODO handle big-endian native order...
        if (ByteOrder.nativeOrder() != ByteOrder.LITTLE_ENDIAN) {
            throw NotImplementedError("Reading PCM data is only supported when native byte order is little-endian.")
        }
        rawByteBuffer.order(ByteOrder.nativeOrder())
        val floatBuffer = rawByteBuffer.asFloatBuffer()
        val numSamples = minOf(floatBuffer.capacity(), maxAudioLengthInSeconds * sampleRate)
        val env = OrtEnvironment.getEnvironment()
        return OnnxTensor.createTensor(
            env, floatBuffer, tensorShape(1, numSamples.toLong())
        )
    }
}

class SpeechRecognizer(modelBytes: ByteArray) : AutoCloseable {
    private val session: OrtSession
    private val baseInputs: Map<String, OnnxTensor>

    init {
        val env = OrtEnvironment.getEnvironment()
        val sessionOptions = OrtSession.SessionOptions()
        sessionOptions.registerCustomOpLibrary(OrtxPackage.getLibraryPath())

        session = env.createSession(modelBytes, sessionOptions)

        val nMels: Long = 80
        val nFrames: Long = 3000

        baseInputs = mapOf(
            "min_length" to createIntTensor(env, intArrayOf(1), tensorShape(1)),
            "max_length" to createIntTensor(env, intArrayOf(200), tensorShape(1)),
            "num_beams" to createIntTensor(env, intArrayOf(1), tensorShape(1)),
            "num_return_sequences" to createIntTensor(env, intArrayOf(1), tensorShape(1)),
            "length_penalty" to createFloatTensor(env, floatArrayOf(1.0f), tensorShape(1)),
            "repetition_penalty" to createFloatTensor(env, floatArrayOf(1.0f), tensorShape(1)),
        )
    }


    fun run(audioTensor: OnnxTensor): Result {
        val inputs = mutableMapOf<String, OnnxTensor>()
        baseInputs.toMap(inputs)
        inputs["audio_pcm"] = audioTensor
        val startTimeInMs = SystemClock.elapsedRealtime()
        val outputs = session.run(inputs)
        val elapsedTimeInMs = SystemClock.elapsedRealtime() - startTimeInMs
        val recognizedText = outputs.use {
            @Suppress("UNCHECKED_CAST")
            (outputs[0].value as Array<Array<String>>)[0][0]
        }
        return Result(recognizedText, elapsedTimeInMs)
    }

    override fun close() {
        baseInputs.values.forEach {
            it.close()
        }
        session.close()
    }
}

class Result(val text: String, val inferenceTimeInMs: Long)