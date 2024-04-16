import 'dart:developer';
import 'dart:io';

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:path_provider/path_provider.dart';
import 'package:record/record.dart';
import 'package:path/path.dart' as p;

class RecordingTab extends StatefulWidget {
  const RecordingTab({Key? key}) : super(key: key);

  @override
  State<RecordingTab> createState() => _RecordingTabState();
}

class _RecordingTabState extends State<RecordingTab> {
  late AudioRecorder audioRecorder;
  static const platform = MethodChannel('medically');
  String text = '';
  @override
  Widget build(BuildContext context) {
    return CupertinoPageScaffold(
      navigationBar: const CupertinoNavigationBar(
        middle: Text('Record'),
      ),
      child: SafeArea(
        child: Center(
          child: Column(
            children: [
              GestureDetector(
                onTap: () async {
                  audioRecorder = AudioRecorder();
                  if (await audioRecorder.hasPermission()) {
                    const encoder = AudioEncoder.pcm16bits;
                    const config =
                        RecordConfig(encoder: encoder, numChannels: 1);
                    final dir = await getApplicationDocumentsDirectory();
                    final path = p.join(
                      dir.path,
                      'audio_${DateTime.now().millisecondsSinceEpoch}.pcm',
                    );
                    await audioRecorder.start(config, path: path);
                  }
                },
                child: const Icon(
                  CupertinoIcons.mic_circle,
                  size: 150,
                  color: Colors.black,
                ),
              ),
              GestureDetector(
                onTap: () async {
                  final path = await audioRecorder.stop();
                  // ignore: unused_local_variable
                  final bytes = await _readFileByte(path!);
                  final result = await platform
                      .invokeMethod<String>('recognize_audio', {"data": bytes});
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

Future<Uint8List?> _readFileByte(String filePath) async {
  Uri myUri = Uri.parse(filePath);
  File audioFile = File.fromUri(myUri);
  Uint8List bytes;
  bytes = await audioFile.readAsBytes();
  return bytes;
}
