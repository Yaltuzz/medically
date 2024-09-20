package com.example.medically

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.nio.FloatBuffer
import java.util.concurrent.atomic.AtomicBoolean

class AudioTensorSource {
    companion object {
        private const val bytesPerFloat = 4
        private const val sampleRate = 16000
        private const val maxAudioLengthInSeconds = 30

        @SuppressLint("MissingPermission")
        fun fromRecording(stopRecordingFlag: AtomicBoolean): OnnxTensor {
            val recordingChunkLengthInSeconds = 1

            val minBufferSize = maxOf(
                AudioRecord.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_FLOAT
                ),
                2 * recordingChunkLengthInSeconds * sampleRate * bytesPerFloat
            )

            val audioRecord = AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBufferSize)
                .build()

            try {
                val floatAudioData = FloatArray(maxAudioLengthInSeconds * sampleRate) { 0.0f }
                var floatAudioDataOffset = 0

                audioRecord.startRecording()

                while (!stopRecordingFlag.get() && floatAudioDataOffset < floatAudioData.size) {
                    val numFloatsToRead = minOf(
                        recordingChunkLengthInSeconds * sampleRate,
                        floatAudioData.size - floatAudioDataOffset
                    )

                    val readResult = audioRecord.read(
                        floatAudioData, floatAudioDataOffset, numFloatsToRead,
                        AudioRecord.READ_BLOCKING
                    )

                    if (readResult >= 0) {
                        floatAudioDataOffset += readResult
                    } else {
                        throw RuntimeException("AudioRecord.read() returned error code $readResult")
                    }
                }

                audioRecord.stop()

                val env = OrtEnvironment.getEnvironment()
                val floatAudioDataBuffer = FloatBuffer.wrap(floatAudioData)

                return OnnxTensor.createTensor(
                    env, floatAudioDataBuffer,
                    tensorShape(1, floatAudioData.size.toLong())
                )

            } finally {
                if (audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop()
                }
                audioRecord.release()
            }
        }
    }

}