import 'dart:developer';

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:record/record.dart';

class TextTab extends StatefulWidget {
  const TextTab({Key? key}) : super(key: key);

  @override
  State<TextTab> createState() => _TextTabState();
}

class _TextTabState extends State<TextTab> {
  late AudioRecorder audioRecorder;
  static const platform = MethodChannel('naturalLanguageProcessing');
  String text = '';
  @override
  Widget build(BuildContext context) {
    return CupertinoPageScaffold(
      navigationBar: const CupertinoNavigationBar(
        middle: Text('Text'),
      ),
      child: SafeArea(
        child: Center(
          child: Column(
            children: [
              GestureDetector(
                onTap: () async {
                  // ignore: unused_local_variable
                  final result = await platform
                      .invokeMethod<String>('interpret_text', {"data": null});
                  log(result ?? '');
                  setState(() {
                    text = result ?? '';
                  });
                },
                child: const Icon(
                  CupertinoIcons.rectangle_fill,
                  size: 30,
                  color: Colors.black,
                ),
              ),
              Text(
                text,
                style: const TextStyle(fontSize: 10),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
