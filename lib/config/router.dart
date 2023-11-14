import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:medically/main_app_screen/main_app_screen.dart';

final appRouterProvider = Provider<GoRouter>(
  (ref) {
    return GoRouter(
      debugLogDiagnostics: kDebugMode,
      routes: [
        GoRoute(
          name: '/',
          path: '/',
          builder: (_, state) {
            return const MainAppScreen();
          },
        ),
      ],
    );
  },
);
