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

  testWidgets('app opens with AgriVision overview', (tester) async {
    SharedPreferences.setMockInitialValues({});
    await tester.pumpWidget(const AgriVisionApp());
    await tester.pumpAndSettle();

    expect(find.text('AgriVision'), findsWidgets);
    expect(find.text('DA Reports'), findsWidgets);
    expect(find.text('Farmer & Farm Registry'), findsOneWidget);
  });
}
