import 'package:flutter/material.dart';

/// Original AgriVision brand mark: sunrise, growing leaves, and field rows.
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
    return Container(
      width: size,
      height: size,
      padding: EdgeInsets.all(size * .055),
      decoration: BoxDecoration(
        color: backgroundColor,
        borderRadius: BorderRadius.circular(size * .25),
      ),
      child: CustomPaint(
        painter: _AgriVisionMarkPainter(onDark: onDark),
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

    const sunColor = Color(0xFFFFBA32);
    final leafColor = onDark ? const Color(0xFF76C86C) : const Color(0xFF267D43);
    final leafLightColor = onDark ? const Color(0xFFB9E69B) : const Color(0xFF3A9C4F);
    final fieldColor = onDark ? const Color(0xFFF6F5EF) : const Color(0xFF173F35);
    final rowColor = onDark ? const Color(0xFF173F35) : const Color(0xFFF6F5EF);

    final sun = Path()
      ..moveTo(13, 52)
      ..cubicTo(16, 27, 33, 10, 54, 10)
      ..cubicTo(76, 10, 93, 26, 96, 49)
      ..lineTo(84, 49)
      ..cubicTo(81, 32, 69, 22, 54, 22)
      ..cubicTo(38, 22, 27, 34, 25, 52)
      ..close();
    canvas.drawPath(sun, Paint()..color = sunColor);

    final field = Path()
      ..moveTo(12, 51)
      ..cubicTo(35, 47, 68, 48, 96, 52)
      ..cubicTo(93, 77, 76, 95, 54, 98)
      ..cubicTo(31, 95, 14, 77, 12, 51)
      ..close();
    canvas.drawPath(field, Paint()..color = fieldColor);

    final rows = Paint()
      ..color = rowColor
      ..style = PaintingStyle.stroke
      ..strokeWidth = 4.4
      ..strokeCap = StrokeCap.round;
    final firstRow = Path()
      ..moveTo(15, 60)
      ..cubicTo(29, 62, 39, 73, 48, 91);
    final secondRow = Path()
      ..moveTo(34, 55)
      ..cubicTo(51, 60, 68, 72, 80, 86);
    final thirdRow = Path()
      ..moveTo(60, 54)
      ..cubicTo(72, 57, 83, 63, 90, 71);
    canvas
      ..drawPath(firstRow, rows)
      ..drawPath(secondRow, rows)
      ..drawPath(thirdRow, rows);

    final leftLeaf = Path()
      ..moveTo(19, 14)
      ..cubicTo(36, 15, 48, 31, 47, 52)
      ..cubicTo(29, 49, 18, 34, 19, 14)
      ..close();
    final rightLeaf = Path()
      ..moveTo(47, 52)
      ..cubicTo(45, 29, 59, 15, 78, 15)
      ..cubicTo(79, 35, 64, 49, 47, 52)
      ..close();
    canvas
      ..drawPath(leftLeaf, Paint()..color = leafColor)
      ..drawPath(rightLeaf, Paint()..color = leafLightColor);

    canvas.restore();
  }

  @override
  bool shouldRepaint(covariant _AgriVisionMarkPainter oldDelegate) =>
      oldDelegate.onDark != onDark;
}
