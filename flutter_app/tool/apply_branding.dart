import 'dart:io';

Future<void> main() async {
  if (!await Directory('android').exists()) {
    stderr.writeln('Android project not found. Run flutter create first.');
    exitCode = 1;
    return;
  }

  const files = <String, String>{
    'branding/android/res/drawable/agrivision_icon_foreground.xml':
        'android/app/src/main/res/drawable/agrivision_icon_foreground.xml',
    'branding/android/res/drawable/agrivision_icon_full.xml':
        'android/app/src/main/res/drawable/agrivision_icon_full.xml',
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

  final manifest = File('android/app/src/main/AndroidManifest.xml');
  if (await manifest.exists()) {
    final original = await manifest.readAsString();
    final branded = original
        .replaceFirst(
          RegExp(r'android:label="[^"]*"'),
          'android:label="AgriVision"',
        )
        .replaceFirst(
          RegExp(r'android:icon="[^"]*"'),
          'android:icon="@drawable/agrivision_icon_full"',
        )
        .replaceFirst(
          RegExp(r'android:roundIcon="[^"]*"'),
          'android:roundIcon="@drawable/agrivision_icon_full"',
        );

    if (branded != original) {
      await manifest.writeAsString(branded);
      stdout.writeln('Set Android app name and launcher icon to AgriVision.');
    } else if (!original.contains('android:label="AgriVision"') ||
        !original.contains('android:icon="@drawable/agrivision_icon_full"')) {
      stderr.writeln('Could not apply AgriVision app name/icon to manifest.');
      exitCode = 1;
      return;
    }
  }
}
