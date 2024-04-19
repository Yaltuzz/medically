import 'dart:developer';

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class TextTab extends StatefulWidget {
  const TextTab({Key? key}) : super(key: key);

  @override
  State<TextTab> createState() => _TextTabState();
}

class _TextTabState extends State<TextTab> {
  final TextEditingController _textController = TextEditingController();
  final TextEditingController _questionController = TextEditingController();
  static const platform = MethodChannel('naturalLanguageProcessing');
  String answer = '';
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          centerTitle: true,
          title: const Text('Q&A'),
        ),
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(20.0),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                TextField(
                    controller: _textController,
                    decoration: const InputDecoration(
                      hintText: 'Enter your text here',
                    ),
                    maxLines: null),
                const SizedBox(height: 50),
                TextField(
                  controller: _questionController,
                  decoration: const InputDecoration(
                    hintText: 'Enter your question here',
                  ),
                ),
                const SizedBox(height: 20),
                ElevatedButton(
                  onPressed: () async {
                    String text = _textController.text;
                    String question = _questionController.text;
                    List<String> textAndQuestion = [text, question];
                    final result = await platform.invokeMethod<String>(
                        'interpret_text', {"data": textAndQuestion});
                    log(result ?? '');
                    print(result ?? 'sdf');
                    setState(() {
                      answer = result ?? '';
                    });
                  },
                  child: const Text('Submit'),
                ),
                Text(
                  answer,
                  style: const TextStyle(fontSize: 10),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
