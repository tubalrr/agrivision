import 'package:flutter/material.dart';

/// AgriVision's green-leaf logo, matching the supplied artwork.
class AgriVisionMark extends StatelessWidget {
  const AgriVisionMark({
    super.key,
    this.size = 44,
    this.backgroundColor = const Color(0xFF173F35),
    this.onDark = true,
  });

  final double size;
  final Color backgroundColor;
  final bool onDark;

  @override
  Widget build(BuildContext context) {
    if (backgroundColor.a == 0.0) {
      // A transparent watermark avoids painting a white square over the page.
      return CustomPaint(
        painter: _AgriVisionMarkPainter(onDark: onDark),
        size: Size.square(size),
      );
    }

    // The supplied logo is a single dark-green leaf on white.
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(size * .25),
      ),
      child: CustomPaint(
        painter: const _AgriVisionMarkPainter(onDark: false),
        size: Size.square(size),
      ),
    );
  }
}

class _AgriVisionMarkPainter extends CustomPainter {
  const _AgriVisionMarkPainter({required this.onDark});

  final bool onDark;

  @override
  void paint(Canvas canvas, Size size) {
    canvas.save();
    canvas.scale(size.width / 100, size.height / 100);

    // Traced from the leaf silhouette and distinctive white slit in the supplied logo.
    final leaf = Path()
      ..moveTo(82.2, 6.5)
      ..cubicTo(72.6, 12.5, 60.3, 17.6, 50.1, 21.5)
      ..cubicTo(41.7, 24.8, 34.5, 29.6, 30, 33.2)
      ..cubicTo(24.3, 38, 19.8, 44, 17.7, 50.3)
      ..cubicTo(14.7, 58.4, 14.4, 67.1, 16.2, 73.7)
      ..cubicTo(17.7, 80, 20.1, 85.4, 22.5, 88.4)
      ..cubicTo(26.4, 77.6, 34.5, 63, 43.2, 52.1)
      ..cubicTo(46.5, 48, 50.1, 44.6, 53.1, 43.4)
      ..lineTo(54, 44.6)
      ..cubicTo(45.6, 56, 38.1, 69.5, 32.7, 82.4)
      ..cubicTo(31.2, 86.6, 30.6, 90.2, 30.3, 92.3)
      ..cubicTo(39, 92.9, 47.7, 91.1, 54.6, 89)
      ..cubicTo(61.8, 86, 67.8, 80.9, 72, 74.6)
      ..cubicTo(77.4, 66.5, 80.7, 55.1, 82.8, 44.9)
      ..cubicTo(84.6, 35, 84.6, 25.1, 84, 19.7)
      ..cubicTo(83.4, 14.6, 82.8, 10.1, 82.2, 6.5)
      ..close();

    canvas.drawPath(
      leaf,
      Paint()..color = onDark ? Colors.white : const Color(0xFF034228),
    );
    canvas.restore();
  }

  @override
  bool shouldRepaint(covariant _AgriVisionMarkPainter oldDelegate) =>
      oldDelegate.onDark != onDark;
}
