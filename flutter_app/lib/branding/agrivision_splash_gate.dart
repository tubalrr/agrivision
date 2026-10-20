import 'dart:async';

import 'package:flutter/material.dart';
import 'package:webview_flutter/webview_flutter.dart';

/// Displays the supplied 3D AgriVision HTML animation while the local app opens.
class AgriVisionSplashGate extends StatefulWidget {
  const AgriVisionSplashGate({required this.child, super.key});

  final Widget child;

  @override
  State<AgriVisionSplashGate> createState() => _AgriVisionSplashGateState();
}

class _AgriVisionSplashGateState extends State<AgriVisionSplashGate> {
  late final WebViewController _controller;
  Timer? _fallbackTimer;
  Timer? _removeTimer;
  bool _showSplash = true;
  bool _splashMounted = true;

  @override
  void initState() {
    super.initState();
    _fallbackTimer = Timer(const Duration(seconds: 12), _dismissSplash);
    _controller = WebViewController()
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..setBackgroundColor(Colors.white)
      ..addJavaScriptChannel(
        'AgriSplashChannel',
        onMessageReceived: (_) => _dismissSplash(),
      )
      ..setNavigationDelegate(
        NavigationDelegate(
          onPageFinished: (_) {
            _fallbackTimer?.cancel();
            _fallbackTimer = Timer(
              const Duration(seconds: 6),
              _dismissSplash,
            );
          },
          onWebResourceError: (_) => _dismissSplash(),
        ),
      )
      ..loadFlutterAsset('assets/agrivision-splash.html');
  }

  void _dismissSplash() {
    if (!mounted || !_showSplash) return;
    _fallbackTimer?.cancel();
    setState(() => _showSplash = false);
    _removeTimer?.cancel();
    _removeTimer = Timer(const Duration(milliseconds: 500), () {
      if (mounted) setState(() => _splashMounted = false);
    });
  }

  @override
  void dispose() {
    _fallbackTimer?.cancel();
    _removeTimer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Stack(
      fit: StackFit.expand,
      children: [
        widget.child,
        if (_splashMounted)
          AnimatedOpacity(
            opacity: _showSplash ? 1 : 0,
            duration: const Duration(milliseconds: 450),
            child: IgnorePointer(
              ignoring: !_showSplash,
              child: ColoredBox(
                color: Colors.white,
                child: WebViewWidget(controller: _controller),
              ),
            ),
          ),
      ],
    );
  }
}
