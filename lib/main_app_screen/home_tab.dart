import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

class HomeTab extends StatelessWidget {
  const HomeTab({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return const CupertinoPageScaffold(
      navigationBar: CupertinoNavigationBar(
        middle: Text('Home'),
      ),
      child: SafeArea(
        child: Center(
          child: Padding(
            padding: EdgeInsets.symmetric(horizontal: 20),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(
                  'Welcome to Medically',
                  style: TextStyle(
                    color: Colors.purple,
                    fontSize: 15,
                    decoration: TextDecoration.none,
                  ),
                ),
                Icon(
                  CupertinoIcons.smiley,
                  color: Colors.purple,
                  size: 50,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
