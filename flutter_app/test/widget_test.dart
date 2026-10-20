import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:agrivision_flutter/main.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  test('CSV export includes headers and all application rows', () {
    const record = FarmerApplication(
      id: 'FAR-100',
      farmerName: 'Ana Dela Cruz',
      contact: '09170000000',
      farmName: 'Green Valley',
      province: 'North Cotabato',
      municipality: 'Antipas',
      barangay: 'Poblacion',
      area: '2 ha',
      landTenure: 'Owned',
      commodities: 'Rice, Corn',
      status: 'For Review',
      reviewNotes: 'Needs land document',
      updatedAt: '2026-10-19T10:00:00',
    );

    final csv = applicationsToCsv([record]).replaceFirst('\uFEFF', '');

    expect(csv, contains('Farmer Name'));
    expect(csv, contains('Ana Dela Cruz'));
    expect(csv, contains('FAR-100'));
    expect(csv, contains('Needs land document'));
    expect(csv.split('\r\n'), hasLength(2));
  });

  test('CSV export escapes commas and quote characters safely', () {
    const record = FarmerApplication(
      id: 'FAR-101',
      farmerName: 'Juan "Jun" Santos',
      contact: '09170000001',
      farmName: 'Farm, North',
      province: 'Province',
      municipality: 'Town',
      barangay: 'Barangay',
      area: '1 ha',
      landTenure: 'Owned',
      commodities: 'Rice',
      status: 'Approved',
      reviewNotes: '',
      updatedAt: '',
    );

    final csv = applicationsToCsv([record]);

    expect(csv, contains('"Juan ""Jun"" Santos"'));
    expect(csv, contains('"Farm, North"'));
  });

  test('farm activity preserves its farmer/farm relationship on persistence', () {
    const activity = FarmActivity(
      id: 'ACT-001',
      farmRecordId: 'FAR-2026-001',
      type: 'Crops',
      title: 'Rice planting',
      category: 'Planting',
      quantity: '1.5',
      unit: 'ha',
      amount: 750.50,
      date: '2026-10-19',
      status: 'In progress',
      notes: 'Linked to the correct farm record',
      updatedAt: '2026-10-19T10:00:00',
    );

    final restored = FarmActivity.fromJson(
      Map<String, dynamic>.from(jsonDecode(jsonEncode(activity.toJson())) as Map),
    );

    expect(restored.id, activity.id);
    expect(restored.farmRecordId, 'FAR-2026-001');
    expect(restored.type, 'Crops');
    expect(restored.title, 'Rice planting');
    expect(restored.amount, 750.50);
  });

  test('local backup exports and restores farmer and farm activity records', () {
    const farmer = FarmerApplication(
      id: 'FAR-BACKUP-1',
      farmerName: 'Maria Santos',
      contact: '09000000000',
      farmName: 'Santos Farm',
      province: 'North Cotabato',
      municipality: 'Antipas',
      barangay: 'Poblacion',
      area: '2 ha',
      landTenure: 'Owned',
      commodities: 'Corn',
      status: 'Approved',
      reviewNotes: '',
      updatedAt: '2026-10-19T10:00:00',
    );
    const activity = FarmActivity(
      id: 'ACT-BACKUP-1',
      farmRecordId: 'FAR-BACKUP-1',
      type: 'Crops',
      title: 'Corn planting',
      category: 'Planting',
      quantity: '2',
      unit: 'ha',
      amount: 0,
      date: '2026-10-19',
      status: 'In progress',
      notes: '',
      updatedAt: '2026-10-19T10:05:00',
    );

    final json = buildAgriVisionBackupJson(
      farmers: [farmer],
      activities: [activity],
      exportedAt: DateTime.parse('2026-10-19T10:10:00'),
    );
    final backup = parseAgriVisionBackupJson(json);

    expect(backup.farmers, hasLength(1));
    expect(backup.farmers.single.farmerName, 'Maria Santos');
    expect(backup.activities, hasLength(1));
    expect(backup.activities.single.farmRecordId, farmer.id);
    expect(backup.activities.single.title, 'Corn planting');
    expect(backup.exportedAt, '2026-10-19T10:10:00.000');
  });

  test('backup import rejects unsupported JSON and orphaned activities', () {
    expect(
      () => parseAgriVisionBackupJson('{"format":"other","schemaVersion":1,"farmers":[],"activities":[]}'),
      throwsFormatException,
    );

    const orphanBackup = '''
    {
      "format": "agrivision.local-backup",
      "schemaVersion": 1,
      "exportedAt": "2026-10-19T10:00:00",
      "farmers": [],
      "activities": [{
        "id": "ACT-ORPHAN",
        "farmRecordId": "MISSING-FARMER",
        "type": "Crops",
        "title": "Unlinked crop",
        "category": "Planting",
        "quantity": "1",
        "unit": "ha",
        "amount": 0,
        "date": "2026-10-19",
        "status": "Planned",
        "notes": "",
        "updatedAt": "2026-10-19T10:00:00"
      }]
    }
    ''';
    expect(() => parseAgriVisionBackupJson(orphanBackup), throwsFormatException);
  });

  testWidgets('app opens with AgriVision overview', (tester) async {
    SharedPreferences.setMockInitialValues({});
    // The overview test doesn't need the native WebView splash plugin.
    await tester.pumpWidget(const AgriVisionApp(showSplash: false));
    await tester.pumpAndSettle();

    expect(find.text('AgriVision'), findsWidgets);
    expect(find.text('DA Reports'), findsWidgets);
    expect(find.text('Registry at a glance'), findsOneWidget);
  });
}
