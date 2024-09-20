import 'package:flutter/cupertino.dart';
import 'package:medically/main_app_screen/forms_tab.dart';
import 'package:medically/main_app_screen/home_tab.dart';

class MainAppScreen extends StatelessWidget {
  const MainAppScreen({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return CupertinoTabScaffold(
      tabBar: CupertinoTabBar(
        items: const [
          BottomNavigationBarItem(
            icon: Icon(CupertinoIcons.home),
            label: 'Home',
          ),
          BottomNavigationBarItem(
            icon: Icon(CupertinoIcons.mic_circle),
            label: 'Record',
          ),
        ],
      ),
      tabBuilder: (BuildContext context, int index) {
        return CupertinoTabView(
          builder: (BuildContext context) {
            switch (index) {
              case 0:
                return const HomeTab();
              case 1:
                return const FormsTab();
              default:
                return Container();
            }
          },
        );
      },
    );
  }
}
