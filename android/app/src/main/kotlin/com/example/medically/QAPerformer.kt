package com.example.medically

import ai.onnxruntime.OrtSession
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import java.util.*

internal data class ResultQA(
        var outputAnswer: String = ""
) {}

internal class QAPerformer(
) {

    fun answer(
            article_seq: CharSequence,
            qustion_seq: CharSequence,
            ortEnv: OrtEnvironment,
            ortSession: OrtSession
    ): ResultQA {
        var result = ResultQA()

        // Step 1: Get article and question as string
        val article = article_seq.toString()
        val question = qustion_seq.toString()

        // Step 2: create shape [batch, sentence_num] and input Tensor
        val shape = longArrayOf(1, 2)
        val inputTensor = OnnxTensor.createTensor(ortEnv, arrayOf(question, article), shape)

        inputTensor.use {
            // Step 3: call ort inferenceSession run
            val output = ortSession.run(Collections.singletonMap("input_text", inputTensor))

            // Step 4: output analysis
            output.use {
                val rawOutput = (output?.get(0)?.value) as Array<String>

                // Step 5: set output result
                result.outputAnswer = rawOutput[0]
            }
        }
        return result
    }
}