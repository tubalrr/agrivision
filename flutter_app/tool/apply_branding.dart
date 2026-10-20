import 'dart:io';

Future<void> main() async {
  if (!await Directory('android').exists()) {
    stderr.writeln('Android project not found. Run flutter create first.');
    exitCode = 1;
    return;
  }

  const files = <String, String>{
    'branding/android/res/drawable/agrivision_leaf_foreground.xml':
        'android/app/src/main/res/drawable/agrivision_leaf_foreground.xml',
    'branding/android/res/drawable/agrivision_leaf_logo.xml':
        'android/app/src/main/res/drawable/agrivision_leaf_logo.xml',
    'branding/android/res/drawable/agrivision_blank_splash.xml':
        'android/app/src/main/res/drawable/agrivision_blank_splash.xml',
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
          'android:icon="@drawable/agrivision_leaf_logo"',
        )
        .replaceFirst(
          RegExp(r'android:roundIcon="[^"]*"'),
          'android:roundIcon="@drawable/agrivision_leaf_logo"',
        );

    if (branded != original) {
      await manifest.writeAsString(branded);
      stdout.writeln('Set AgriVision app name and leaf launcher icon.');
    } else if (!original.contains('android:label="AgriVision"') ||
        !original.contains('android:icon="@drawable/agrivision_leaf_logo"')) {
      stderr.writeln(
        'Could not apply AgriVision app name/leaf icon to manifest.',
      );
      exitCode = 1;
      return;
    }
  }

  // Android 12+ draws the launcher icon in its mandatory system splash screen
  // before Flutter starts. Use a transparent drawable there so the old logo
  // cannot flash briefly before the AgriVision animated intro appears.
  const launchStyle = '''<style name="LaunchTheme" parent="@android:style/Theme.Light.NoTitleBar">
        <item name="android:windowBackground">@drawable/launch_background</item>
        <item name="android:windowSplashScreenBackground">#FFFFFFFF</item>
        <item name="android:windowSplashScreenAnimatedIcon">@drawable/agrivision_blank_splash</item>
        <item name="android:windowSplashScreenIconBackgroundColor">#FFFFFFFF</item>
    </style>''';

  for (final relativePath in [
    'android/app/src/main/res/values-v31/styles.xml',
    'android/app/src/main/res/values-night-v31/styles.xml',
  ]) {
    final themeFile = File(relativePath);
    await themeFile.parent.create(recursive: true);
    var xml = await themeFile.exists()
        ? await themeFile.readAsString()
        : '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n</resources>\n';

    final launchStylePattern = RegExp(
      r'<style\\s+name="LaunchTheme"[^>]*>[\\s\\S]*?</style>',
    );
    if (launchStylePattern.hasMatch(xml)) {
      xml = xml.replaceFirst(launchStylePattern, launchStyle);
    } else {
      xml = xml.replaceFirst('</resources>', '    $launchStyle\n</resources>');
    }
    await themeFile.writeAsString(xml);
    stdout.writeln('Removed Android system splash logo: $relativePath');
  }
}
