import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:record/record.dart';

class FormsTab extends StatefulWidget {
  const FormsTab({Key? key}) : super(key: key);

  @override
  State<FormsTab> createState() => _RecordingTabState();
}

class _RecordingTabState extends State<FormsTab> {
  // Audio tools
  late AudioRecorder audioRecorder;
  bool isRecording = false;
  bool isAudioProcessing = false;
  static const platformAsr = MethodChannel('automaticSpeechRecognition');
  String? text = '';
  static const MethodChannel _channel = MethodChannel('flutter_channel');

  // Forms tools
  final TextEditingController _textController = TextEditingController();
  final TextEditingController _nameController = TextEditingController();
  final TextEditingController _genderController = TextEditingController();
  final TextEditingController _symptomsController = TextEditingController();
  final TextEditingController _ageController = TextEditingController();
  bool isLoading = false;
  static const platformNlp = MethodChannel('naturalLanguageProcessing');

  void waitForResults() {
    _channel.setMethodCallHandler((MethodCall call) async {
      if (call.method == 'flutterMethod') {
        setState(() {
          isAudioProcessing = false;
          _textController.text =
              (call.arguments as String?) ?? "Nie rozpoznano mowy";
          print(_textController.text);
        });
      }
    });
  }

  @override
  void initState() {
    super.initState();
    waitForResults();
  }

  @override
  Widget build(BuildContext context) {
    return CupertinoPageScaffold(
      navigationBar: const CupertinoNavigationBar(
        middle: Text('Formularze'),
      ),
      child: SafeArea(
        child: Material(
          child: Center(
            child: SingleChildScrollView(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 20),
                child: Column(
                  children: [
                    const SizedBox(
                      height: 20,
                    ),
                    const Text(
                      'Nagraj wypowiedź lekarską',
                      style: TextStyle(
                          fontFamily: 'roboto',
                          color: Colors.black,
                          decoration: TextDecoration.none,
                          fontSize: 20),
                    ),
                    const Text(
                      'Wypowiedź moze trwać maksymalnie 30 sekund',
                      style: TextStyle(
                          fontFamily: 'roboto',
                          color: Colors.black,
                          decoration: TextDecoration.none,
                          fontSize: 10),
                    ),
                    GestureDetector(
                      onTap: () async {
                        if (isRecording) {
                          platformAsr.invokeMethod<String>(
                            'stop_recording',
                          );
                          setState(() {
                            isRecording = false;
                            isAudioProcessing = true;
                          });
                        } else {
                          audioRecorder = AudioRecorder();
                          if (await audioRecorder.hasPermission()) {
                            setState(() {
                              isRecording = true;
                            });
                            platformAsr.invokeMethod<String>(
                              'start_recording',
                            );
                          }
                        }
                      },
                      child: !isRecording
                          ? const Icon(
                              CupertinoIcons.play,
                              size: 50,
                              color: Colors.black,
                            )
                          : const Icon(
                              CupertinoIcons.stop,
                              size: 50,
                              color: Colors.black,
                            ),
                    ),
                    const Text(
                      'Wynik przetwarzania dźwięku, popraw ewentualne błędy i zatwierdź aby wypełniły się formularze',
                      style: TextStyle(
                          fontFamily: 'roboto',
                          color: Colors.black,
                          decoration: TextDecoration.none,
                          fontSize: 20),
                    ),
                    TextField(
                        controller: _textController,
                        decoration: InputDecoration(
                          hintText: isAudioProcessing
                              ? "Loading..."
                              : 'Tutaj pojawi się wypowiedź',
                        ),
                        maxLines: null),
                    const SizedBox(height: 20),
                    if (!isLoading)
                      Center(
                        child: ElevatedButton(
                          onPressed: () async {
                            isLoading = true;
                            String text = _textController.text;
                            String question = 'Jak nazywa się pacjent';
                            final name = await platformNlp
                                .invokeMethod<String>('answer', {
                              "context": text,
                              "question": question,
                            });

                            question = 'Ile lat ma pacjent';
                            final age = await platformNlp
                                .invokeMethod<String>('answer', {
                              "context": text,
                              "question": question,
                            });

                            question = 'Jaką płeć ma pacjent';
                            final gender = await platformNlp
                                .invokeMethod<String>('answer', {
                              "context": text,
                              "question": question,
                            });

                            question = 'Jakie objawy ma pacjent';
                            final symptopms = await platformNlp
                                .invokeMethod<String>('answer', {
                              "context": text,
                              "question": question,
                            });

                            setState(() {
                              _nameController.text = name ?? '';
                              _ageController.text = age ?? '';
                              _genderController.text = gender ?? '';
                              _symptomsController.text = symptopms ?? '';
                            });
                            isLoading = false;
                          },
                          child: const Text('Przeanalizuj'),
                        ),
                      )
                    else
                      const Text(
                        'Loading ...',
                      ),
                    const SizedBox(height: 40),
                    const Text('Imię i nazwisko pacjenta:'),
                    TextField(
                      controller: _nameController,
                    ),
                    const SizedBox(height: 20),
                    const Text('Płeć pacjenta:'),
                    TextField(
                      controller: _genderController,
                    ),
                    const SizedBox(height: 20),
                    const Text('Wiek pacjenta:'),
                    TextField(
                      controller: _ageController,
                    ),
                    const SizedBox(height: 20),
                    const Text('Objawy pacjenta:'),
                    TextField(
                      controller: _symptomsController,
                    ),
                    const SizedBox(height: 20),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
