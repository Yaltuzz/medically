import 'dart:developer';

import 'package:chaquopy/chaquopy.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import 'package:record/record.dart';
import 'package:path/path.dart' as p;
import 'package:audioplayers/audioplayers.dart' as ap;
import 'package:audioplayers/audioplayers.dart';

class RecordingTab extends StatefulWidget {
  const RecordingTab({Key? key}) : super(key: key);

  @override
  State<RecordingTab> createState() => _RecordingTabState();
}

class _RecordingTabState extends State<RecordingTab> {
  late AudioRecorder audioRecorder;
  final _audioPlayer = ap.AudioPlayer()..setReleaseMode(ReleaseMode.stop);

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
                  const code = 'import numpy as np\n'
                      'arr = np.array([1, 2, 3, 4, 5])\n'
                      'print(arr)\n'
                      'print(type(arr))\n';
                  final res = await Chaquopy.executeCode(code);
                  log(res.toString());
                  audioRecorder = AudioRecorder();
                  if (await audioRecorder.hasPermission()) {
                    const encoder = AudioEncoder.wav;

                    final devs = await audioRecorder.listInputDevices();
                    debugPrint(devs.toString());

                    const config =
                        RecordConfig(encoder: encoder, numChannels: 1);

                    final dir = await getApplicationDocumentsDirectory();
                    final path = p.join(
                      dir.path,
                      'audio_${DateTime.now().millisecondsSinceEpoch}.m4a',
                    );
                    await audioRecorder.start(config, path: path);
                  }
                },
                onLongPress: () async {
                  final path = await audioRecorder.stop();
                  _audioPlayer.setSource(DeviceFileSource(path!));
                  _audioPlayer.play(DeviceFileSource(path), volume: 100);
                },
                child: const Icon(
                  CupertinoIcons.mic_circle,
                  size: 150,
                  color: Colors.black,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
