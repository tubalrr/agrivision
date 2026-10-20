import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:agrivision_flutter/main.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  test('CSV export includes farmer profile headers and fields', () {
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

    final csv = farmProfilesToCsv([record]).replaceFirst('\uFEFF', '');

    expect(csv, contains('Farmer Name'));
    expect(csv, contains('Ana Dela Cruz'));
    expect(csv, contains('FAR-100'));
    expect(csv, contains('Green Valley'));
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

    final csv = farmProfilesToCsv([record]);

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

  test('offline summary calculates cash flow, due tasks, and inventory balances', () {
    final rows = [
      const FarmActivity(
        id: 'INC-1', farmRecordId: 'F1', type: 'Finance', title: 'Rice sale',
        category: 'Income / sale', quantity: '1', unit: 'lot', amount: 12000,
        date: '2026-10-20', status: 'Received', notes: '', updatedAt: '',
      ),
      const FarmActivity(
        id: 'EXP-1', farmRecordId: 'F1', type: 'Finance', title: 'Fertilizer',
        category: 'Expense', quantity: '2', unit: 'bags', amount: 3500,
        date: '2026-10-20', status: 'Paid', notes: '', updatedAt: '',
      ),
      const FarmActivity(
        id: 'TASK-1', farmRecordId: 'F1', type: 'Tasks', title: 'Water the field',
        category: 'Watering', quantity: '', unit: '', amount: 0,
        date: '2026-10-19', status: 'Pending', notes: '', updatedAt: '',
      ),
      const FarmActivity(
        id: 'TASK-2', farmRecordId: 'F1', type: 'Tasks', title: 'Check seedlings',
        category: 'Follow-up', quantity: '', unit: '', amount: 0,
        date: '2026-10-20', status: 'In progress', notes: '', updatedAt: '',
      ),
      const FarmActivity(
        id: 'STOCK-IN', farmRecordId: 'F1', type: 'Inventory', title: 'Urea',
        category: 'Stock In / purchase', quantity: '10', unit: 'kg', amount: 0,
        date: '2026-10-20', status: 'In stock', notes: '', updatedAt: '',
      ),
      const FarmActivity(
        id: 'STOCK-OUT', farmRecordId: 'F1', type: 'Inventory', title: 'Urea',
        category: 'Stock Out / used', quantity: '10', unit: 'kg', amount: 0,
        date: '2026-10-20', status: 'Out of stock', notes: '', updatedAt: '',
      ),
      const FarmActivity(
        id: 'PROD-1', farmRecordId: 'F1', type: 'Production', title: 'Rice harvest',
        category: 'Harvest', quantity: '500', unit: 'kg', amount: 0,
        date: '2026-10-20', status: 'Recorded', notes: '', updatedAt: '',
      ),
    ];
    final summary = calculateOfflineFarmSummary(rows, now: DateTime(2026, 10, 20));
    expect(summary.income, 12000);
    expect(summary.expenses, 3500);
    expect(summary.netCashFlow, 8500);
    expect(summary.overdueTasks, 1);
    expect(summary.tasksDueToday, 1);
    expect(summary.inventoryItems, 1);
    expect(summary.lowOrEmptyStockItems, 1);
    expect(summary.productionRecords, 1);
  });

  test('backup preserves offline module types and schema v1 compatibility', () {
    const farmer = FarmerApplication(
      id: 'FARM-1', farmerName: 'Ana', contact: '', farmName: 'Farm',
      province: '', municipality: '', barangay: '', area: '',
      landTenure: 'Owned', commodities: 'Rice', status: '', reviewNotes: '',
      updatedAt: '',
    );
    const field = FarmActivity(
      id: 'FIELD-1', farmRecordId: 'FARM-1', type: 'Field Mapping',
      title: 'North field point', category: 'Field point / coordinate',
      quantity: '1', unit: 'point', amount: 0, date: '2026-10-20',
      status: 'Recorded', notes: '7.1234, 125.5678', updatedAt: '',
    );
    final newBackup = buildAgriVisionBackupJson(farmers: [farmer], activities: [field]);
    expect(parseAgriVisionBackupJson(newBackup).activities.single.type, 'Field Mapping');
    final v1 = newBackup.replaceFirst('"schemaVersion": 2', '"schemaVersion": 1');
    expect(parseAgriVisionBackupJson(v1).activities.single.title, 'North field point');
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
    expect(find.text('My Reports'), findsWidgets);
    expect(find.text('My farm at a glance'), findsOneWidget);
    await tester.tap(find.text('More').last);
    await tester.pumpAndSettle();
    expect(find.text('Offline farm insights'), findsOneWidget);
  });
}
