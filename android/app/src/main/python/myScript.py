import torch
from transformers import DistilBertTokenizerFast,DistilBertForQuestionAnswering,pipeline,AutoProcessor, AutoModelForSpeechSeq2Seq
import os
import io

path = os.path.dirname(__file__)
model_path = os.path.join(path,'nlp_model')
asr_model_path = os.path.join(path,'whisper-small-model')

tokenizer = DistilBertTokenizerFast.from_pretrained(model_path)
model = DistilBertForQuestionAnswering.from_pretrained(model_path)

def predict(question,context):
	inputs = tokenizer(question, context, return_tensors="pt")
    
	with torch.no_grad():
		outputs = model(**inputs)

	start_index = torch.argmax(outputs.start_logits)
	end_index = torch.argmax(outputs.end_logits)

	all_tokens = tokenizer.convert_ids_to_tokens(inputs['input_ids'][0])
	answer = tokenizer.convert_tokens_to_string(all_tokens[start_index:end_index + 1])
	print(answer)
	return answer
