import 'dart:io';

import 'package:image/image.dart' as img;

Future<void> main() async {
  if (!await Directory('android').exists()) {
    stderr.writeln('Android project not found. Run flutter create first.');
    exitCode = 1;
    return;
  }

  const files = <String, String>{
    'branding/android/res/drawable/agrivision_icon_foreground.xml':
        'android/app/src/main/res/drawable/agrivision_icon_foreground.xml',
    'branding/android/res/values/agrivision_brand_colors.xml':
        'android/app/src/main/res/values/agrivision_brand_colors.xml',
    'branding/android/res/mipmap-anydpi-v26/ic_launcher.xml':
        'android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml',
    'branding/android/res/mipmap-anydpi-v26/ic_launcher_round.xml':
        'android/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml',
  };

  for (final entry in files.entries) {
    final source = File(entry.key);
    if (!await source.exists()) {
      stderr.writeln('Missing branding source: ${entry.key}');
      exitCode = 1;
      return;
    }
    final destination = File(entry.value);
    await destination.parent.create(recursive: true);
    await source.copy(destination.path);
    stdout.writeln('Applied: ${entry.value}');
  }

  final iconFile = File('branding/agrivision_icon.png');
  if (!await iconFile.exists()) {
    stderr.writeln('Missing supplied logo: ${iconFile.path}');
    exitCode = 1;
    return;
  }
  final iconBytes = await iconFile.readAsBytes();
  final icon = img.decodePng(iconBytes);
  if (icon == null) {
    stderr.writeln('Could not decode branding/agrivision_icon.png.');
    exitCode = 1;
    return;
  }

  // Use the supplied square logo as the adaptive-icon foreground.
  final foreground = File(
    'android/app/src/main/res/drawable-nodpi/agrivision_launcher_foreground.png',
  );
  await foreground.parent.create(recursive: true);
  await foreground.writeAsBytes(iconBytes);
  stdout.writeln('Applied: ${foreground.path}');

  // Supply correctly sized legacy launcher icons for Android versions before API 26.
  const launcherSizes = <String, int>{
    'mdpi': 48,
    'hdpi': 72,
    'xhdpi': 96,
    'xxhdpi': 144,
    'xxxhdpi': 192,
  };
  for (final entry in launcherSizes.entries) {
    final directory = Directory(
      'android/app/src/main/res/mipmap-${entry.key}',
    );
    await directory.create(recursive: true);
    final resized = img.copyResize(
      icon,
      width: entry.value,
      height: entry.value,
      interpolation: img.Interpolation.cubic,
    );
    final pngBytes = img.encodePng(resized);
    for (final name in const ['ic_launcher.png', 'ic_launcher_round.png']) {
      final output = File('${directory.path}/$name');
      await output.writeAsBytes(pngBytes);
    }
  }
  stdout.writeln('Generated launcher icons from the supplied AgriVision logo.');

  final manifest = File('android/app/src/main/AndroidManifest.xml');
  if (await manifest.exists()) {
    final original = await manifest.readAsString();
    final branded = original.replaceFirst(
      RegExp(r'android:label="[^"]*"'),
      'android:label="AgriVision"',
    );
    if (branded != original) {
      await manifest.writeAsString(branded);
      stdout.writeln('Set Android app name to AgriVision.');
    } else if (!original.contains('android:label="AgriVision"')) {
      stderr.writeln('Could not find Android application label in manifest.');
      exitCode = 1;
      return;
    }
  }
}
