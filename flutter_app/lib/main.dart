import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:file_picker/file_picker.dart';
import 'package:share_plus/share_plus.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:pdf/pdf.dart';
import 'package:pdf/widgets.dart' as pw;
import 'branding/agrivision_mark.dart';
import 'branding/agrivision_splash_gate.dart';

const _forest = Color(0xFF173F35);
const _green = Color(0xFF2F7559);
const _mint = Color(0xFFE1F0E6);
const _cream = Color(0xFFF6F5EF);
const _ink = Color(0xFF1C2B25);
const _muted = Color(0xFF718078);
const _line = Color(0xFFE4E9E3);
const _amber = Color(0xFFB87922);

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const AgriVisionApp());
}

class FarmerApplication {
  const FarmerApplication({
    required this.id,
    required this.farmerName,
    required this.contact,
    required this.farmName,
    required this.province,
    required this.municipality,
    required this.barangay,
    required this.area,
    required this.landTenure,
    required this.commodities,
    required this.status,
    required this.reviewNotes,
    required this.updatedAt,
  });

  final String id;
  final String farmerName;
  final String contact;
  final String farmName;
  final String province;
  final String municipality;
  final String barangay;
  final String area;
  final String landTenure;
  final String commodities;
  final String status;
  final String reviewNotes;
  final String updatedAt;

  String get location => [barangay, municipality, province]
      .where((part) => part.trim().isNotEmpty)
      .join(', ');

  factory FarmerApplication.fromJson(Map<String, dynamic> json) {
    String value(String key) => (json[key] ?? '').toString();
    return FarmerApplication(
      id: value('id'),
      farmerName: value('farmerName'),
      contact: value('contact'),
      farmName: value('farmName'),
      province: value('province'),
      municipality: value('municipality'),
      barangay: value('barangay'),
      area: value('area'),
      landTenure: value('landTenure'),
      commodities: value('commodities'),
      status: value('status').isEmpty ? 'For Review' : value('status'),
      reviewNotes: value('reviewNotes'),
      updatedAt: value('updatedAt'),
    );
  }

  Map<String, dynamic> toJson() => {
        'id': id,
        'farmerName': farmerName,
        'contact': contact,
        'farmName': farmName,
        'province': province,
        'municipality': municipality,
        'barangay': barangay,
        'area': area,
        'landTenure': landTenure,
        'commodities': commodities,
        'status': status,
        'reviewNotes': reviewNotes,
        'updatedAt': updatedAt,
      };

  FarmerApplication copyWith({
    String? id,
    String? farmerName,
    String? contact,
    String? farmName,
    String? province,
    String? municipality,
    String? barangay,
    String? area,
    String? landTenure,
    String? commodities,
    String? status,
    String? reviewNotes,
    String? updatedAt,
  }) =>
      FarmerApplication(
        id: id ?? this.id,
        farmerName: farmerName ?? this.farmerName,
        contact: contact ?? this.contact,
        farmName: farmName ?? this.farmName,
        province: province ?? this.province,
        municipality: municipality ?? this.municipality,
        barangay: barangay ?? this.barangay,
        area: area ?? this.area,
        landTenure: landTenure ?? this.landTenure,
        commodities: commodities ?? this.commodities,
        status: status ?? this.status,
        reviewNotes: reviewNotes ?? this.reviewNotes,
        updatedAt: updatedAt ?? this.updatedAt,
      );
}

String farmProfilesToCsv(List<FarmerApplication> records) {
  String cell(String value) {
    // Prevent spreadsheet formula execution for untrusted user-entered text.
    final dangerous = RegExp(r'^[\s\uFEFF]*[=+\-@＝＋－＠]').hasMatch(value);
    final safe = dangerous ? "'$value" : value;
    return '"${safe.replaceAll('"', '""')}"';
  }
  final rows = <List<String>>[
    [
      'Farm Record ID',
      'Farmer Name',
      'Contact Number',
      'Farm Name',
      'Province',
      'Municipality/City',
      'Barangay',
      'Farm Area',
      'Land Tenure',
      'Commodities',
      'Last Updated',
    ],
    ...records.map((r) => [
          r.id,
          r.farmerName,
          r.contact,
          r.farmName,
          r.province,
          r.municipality,
          r.barangay,
          r.area,
          r.landTenure,
          r.commodities,
          r.updatedAt,
        ]),
  ];
  return '\uFEFF${rows.map((row) => row.map(cell).join(',')).join('\r\n')}';
}

class FarmActivity {
  const FarmActivity({
    required this.id,
    required this.farmRecordId,
    required this.type,
    required this.title,
    required this.category,
    required this.quantity,
    required this.unit,
    required this.amount,
    required this.date,
    required this.status,
    required this.notes,
    required this.updatedAt,
  });

  final String id;
  final String farmRecordId;
  final String type;
  final String title;
  final String category;
  final String quantity;
  final String unit;
  final double amount;
  final String date;
  final String status;
  final String notes;
  final String updatedAt;

  factory FarmActivity.fromJson(Map<String, dynamic> json) {
    String value(String key) => (json[key] ?? '').toString();
    return FarmActivity(
      id: value('id'),
      farmRecordId: value('farmRecordId'),
      type: value('type'),
      title: value('title'),
      category: value('category'),
      quantity: value('quantity'),
      unit: value('unit'),
      amount: double.tryParse(value('amount')) ?? 0,
      date: value('date'),
      status: value('status'),
      notes: value('notes'),
      updatedAt: value('updatedAt'),
    );
  }

  Map<String, dynamic> toJson() => {
    'id': id,
    'farmRecordId': farmRecordId,
    'type': type,
    'title': title,
    'category': category,
    'quantity': quantity,
    'unit': unit,
    'amount': amount,
    'date': date,
    'status': status,
    'notes': notes,
    'updatedAt': updatedAt,
  };
}


class OfflineFarmSummary {
  const OfflineFarmSummary({
    required this.income,
    required this.expenses,
    required this.overdueTasks,
    required this.tasksDueToday,
    required this.inventoryItems,
    required this.lowOrEmptyStockItems,
    required this.productionRecords,
    required this.fieldRecords,
    required this.incidentRecords,
  });

  final double income;
  final double expenses;
  final int overdueTasks;
  final int tasksDueToday;
  final int inventoryItems;
  final int lowOrEmptyStockItems;
  final int productionRecords;
  final int fieldRecords;
  final int incidentRecords;

  double get netCashFlow => income - expenses;
}

/// Summarizes only saved local records; no internet connection is needed.
OfflineFarmSummary calculateOfflineFarmSummary(
  Iterable<FarmActivity> activities, {
  DateTime? now,
}) {
  final rows = activities.toList(growable: false);
  final dateValue = now ?? DateTime.now();
  final today = DateTime(dateValue.year, dateValue.month, dateValue.day);
  var income = 0.0;
  var expenses = 0.0;
  var overdue = 0;
  var dueToday = 0;
  final stock = <String, double>{};

  for (final row in rows) {
    final category = row.category.toLowerCase();
    final status = row.status.toLowerCase();

    if (row.type == 'Finance') {
      if (category.contains('income') || category.contains('sale') || status == 'received') {
        income += row.amount;
      } else if (category.contains('expense') || category.contains('cost') ||
          category.contains('labor') || category.contains('fuel') ||
          category.contains('medicine')) {
        expenses += row.amount;
      }
    }

    if (row.type == 'Tasks' && status != 'completed' && status != 'cancelled') {
      final date = DateTime.tryParse(row.date);
      if (date != null) {
        final due = DateTime(date.year, date.month, date.day);
        if (due.isBefore(today)) overdue++;
        if (due == today) dueToday++;
      }
    }

    if (row.type == 'Inventory') {
      final key = '${row.title.trim().toLowerCase()}|${row.unit.trim().toLowerCase()}';
      final quantity = double.tryParse(row.quantity.trim());
      if (quantity != null && quantity.isFinite && quantity >= 0) {
        if (category.contains('stock in') || category.contains('purchase') ||
            category.contains('restock') || category == 'add stock') {
          stock[key] = (stock[key] ?? 0) + quantity;
        } else if (category.contains('stock out') || category.contains('used') ||
            category.contains('issued') || category.contains('wastage')) {
          stock[key] = (stock[key] ?? 0) - quantity;
        }
      }
    }
  }

  return OfflineFarmSummary(
    income: income,
    expenses: expenses,
    overdueTasks: overdue,
    tasksDueToday: dueToday,
    inventoryItems: stock.length,
    lowOrEmptyStockItems: stock.values.where((value) => value <= 0).length,
    productionRecords: rows.where((row) => row.type == 'Production').length,
    fieldRecords: rows.where((row) => row.type == 'Field Mapping').length,
    incidentRecords: rows.where((row) =>
        row.type == 'Incidents' || row.type == 'Pest & Disease').length,
  );
}

class AgriVisionBackup {
  const AgriVisionBackup({
    required this.exportedAt,
    required this.farmers,
    required this.activities,
  });

  final String exportedAt;
  final List<FarmerApplication> farmers;
  final List<FarmActivity> activities;
}

String buildAgriVisionBackupJson({
  required List<FarmerApplication> farmers,
  required List<FarmActivity> activities,
  DateTime? exportedAt,
}) {
  final timestamp = (exportedAt ?? DateTime.now()).toIso8601String();
  return const JsonEncoder.withIndent('  ').convert({
    'format': 'agrivision.local-backup',
    'schemaVersion': 2,
    'app': 'AgriVision',
    'exportedAt': timestamp,
    'recordCounts': {
      'farmers': farmers.length,
      'activities': activities.length,
    },
    'farmers': farmers.map((record) => record.toJson()).toList(),
    'activities': activities.map((activity) => activity.toJson()).toList(),
  });
}

AgriVisionBackup parseAgriVisionBackupJson(String content) {
  if (content.length > 10 * 1024 * 1024) {
    throw const FormatException('Backup is larger than the 10 MB safety limit.');
  }

  dynamic decoded;
  try {
    decoded = jsonDecode(content);
  } on FormatException {
    throw const FormatException('This file is not valid JSON.');
  }
  if (decoded is! Map) {
    throw const FormatException('The backup must be a JSON object.');
  }
  final root = Map<String, dynamic>.from(decoded);
  if (root['format'] != 'agrivision.local-backup' ||
      !const {1, 2}.contains(root['schemaVersion'])) {
    throw const FormatException(
      'Unsupported backup format or version. Choose an AgriVision backup file.',
    );
  }
  if (root['farmers'] is! List || root['activities'] is! List) {
    throw const FormatException('The backup is missing farmer or activity records.');
  }

  final farmers = <FarmerApplication>[];
  final farmerIds = <String>{};
  for (final item in root['farmers'] as List<dynamic>) {
    if (item is! Map) {
      throw const FormatException('A farmer record is malformed.');
    }
    final farmer = FarmerApplication.fromJson(Map<String, dynamic>.from(item));
    if (farmer.id.trim().isEmpty || farmer.farmerName.trim().isEmpty) {
      throw const FormatException('Every farmer must have a record ID and name.');
    }
    if (!farmerIds.add(farmer.id)) {
      throw FormatException('Duplicate farmer record ID: ${farmer.id}');
    }
    farmers.add(farmer);
  }

  final activities = <FarmActivity>[];
  final activityIds = <String>{};
  const allowedTypes = {
    'Crops', 'Livestock', 'Finance', 'Tasks', 'Inventory', 'Production',
    'Equipment', 'Field Mapping', 'Pest & Disease', 'Incidents',
  };
  for (final item in root['activities'] as List<dynamic>) {
    if (item is! Map) {
      throw const FormatException('A farm activity record is malformed.');
    }
    final map = Map<String, dynamic>.from(item);
    final activity = FarmActivity.fromJson(map);
    if (activity.id.trim().isEmpty ||
        activity.farmRecordId.trim().isEmpty ||
        activity.title.trim().isEmpty) {
      throw const FormatException('Every farm activity needs an ID, farmer link, and title.');
    }
    if (!activityIds.add(activity.id)) {
      throw FormatException('Duplicate activity record ID: ${activity.id}');
    }
    if (!allowedTypes.contains(activity.type)) {
      throw FormatException('Unknown farm activity type: ${activity.type}');
    }
    if (!farmerIds.contains(activity.farmRecordId)) {
      throw FormatException(
        'Activity "${activity.title}" is linked to a farmer that is missing from this backup.',
      );
    }
    final rawAmount = map['amount'];
    if (rawAmount != null) {
      final parsedAmount = double.tryParse(rawAmount.toString());
      if (parsedAmount == null || !parsedAmount.isFinite || parsedAmount < 0) {
        throw FormatException('Invalid amount on activity "${activity.title}".');
      }
    }
    activities.add(activity);
  }

  return AgriVisionBackup(
    exportedAt: (root['exportedAt'] ?? '').toString(),
    farmers: farmers,
    activities: activities,
  );
}

class AgriVisionApp extends StatelessWidget {
  const AgriVisionApp({super.key, this.showSplash = true});

  final bool showSplash;

  @override
  Widget build(BuildContext context) {
    final scheme = ColorScheme.fromSeed(
      seedColor: _green,
      brightness: Brightness.light,
      surface: Colors.white,
    );
    return MaterialApp(
      title: 'AgriVision',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: scheme,
        scaffoldBackgroundColor: _cream,
        fontFamily: 'Roboto',
        appBarTheme: const AppBarTheme(
          backgroundColor: _cream,
          foregroundColor: _ink,
          elevation: 0,
          centerTitle: false,
        ),
        cardTheme: CardThemeData(
          color: Colors.white,
          elevation: 0,
          margin: EdgeInsets.zero,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(22),
            side: const BorderSide(color: _line),
          ),
        ),
        inputDecorationTheme: InputDecorationTheme(
          filled: true,
          fillColor: const Color(0xFFF8FAF7),
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(14),
            borderSide: const BorderSide(color: _line),
          ),
          enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(14),
            borderSide: const BorderSide(color: _line),
          ),
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(14),
            borderSide: const BorderSide(color: _green, width: 1.6),
          ),
        ),
      ),
      home: showSplash
          ? const AgriVisionSplashGate(child: AgriHome())
          : const AgriHome(),
    );
  }
}

class AgriHome extends StatefulWidget {
  const AgriHome({super.key});

  @override
  State<AgriHome> createState() => _AgriHomeState();
}

class _AgriHomeState extends State<AgriHome> {
  int _tab = 0;
  bool _loading = true;
  String _search = '';
  String _activitySearch = '';
  String _activityType = 'Crops';
  String _selectedFarmRecordId = '';
  String _reportType = 'Farm Profile';
  String _reportFormat = 'CSV';
  String _reportScope = 'All my farms';
  List<FarmerApplication> _records = [];
  List<FarmActivity> _activities = [];

  @override
  void initState() {
    super.initState();
    _loadRecords();
  }

  Future<void> _loadRecords() async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString('agrivision_flutter_registry_v1');
    final activityRaw = prefs.getString('agrivision_flutter_operations_v1');
    final records = <FarmerApplication>[];
    final activities = <FarmActivity>[];
    if (raw != null) {
      try {
        final decoded = jsonDecode(raw) as List<dynamic>;
        records.addAll(decoded.map((e) =>
            FarmerApplication.fromJson(Map<String, dynamic>.from(e as Map))));
      } catch (_) {
        // Keep the app usable if a previous local cache is malformed.
      }
    }
    if (activityRaw != null) {
      try {
        final decoded = jsonDecode(activityRaw) as List<dynamic>;
        activities.addAll(decoded.map((e) =>
            FarmActivity.fromJson(Map<String, dynamic>.from(e as Map))));
      } catch (_) {
        // Keep valid registry records even if the activity cache is invalid.
      }
    }
    if (!mounted) return;
    setState(() {
      _records = records;
      _activities = activities;
      _loading = false;
    });
  }

  Future<void> _persist() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(
      'agrivision_flutter_registry_v1',
      jsonEncode(_records.map((r) => r.toJson()).toList()),
    );
  }

  Future<void> _persistActivities() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(
      'agrivision_flutter_operations_v1',
      jsonEncode(_activities.map((a) => a.toJson()).toList()),
    );
  }

  List<FarmActivity> get _filteredActivities {
    final selectedId = _records.any((r) => r.id == _selectedFarmRecordId)
        ? _selectedFarmRecordId
        : (_records.isNotEmpty ? _records.first.id : '');
    final q = _activitySearch.trim().toLowerCase();
    return _activities.where((activity) {
      if (activity.farmRecordId != selectedId) return false;
      if (activity.type != _activityType) return false;
      if (q.isEmpty) return true;
      return [
        activity.title, activity.category, activity.date, activity.status,
        activity.notes, activity.quantity, activity.unit,
      ].any((value) => value.toLowerCase().contains(q));
    }).toList()
      ..sort((a, b) => b.updatedAt.compareTo(a.updatedAt));
  }

  FarmerApplication? get _selectedFarmRecord {
    for (final record in _records) {
      if (record.id == _selectedFarmRecordId) return record;
    }
    return _records.isEmpty ? null : _records.first;
  }

  List<FarmerApplication> get _filteredRecords {
    final q = _search.trim().toLowerCase();
    if (q.isEmpty) return _records;
    return _records.where((r) => [
      r.farmerName, r.id, r.contact, r.farmName, r.location, r.commodities, r.status,
    ].any((v) => v.toLowerCase().contains(q))).toList();
  }

  Future<void> _openApplication({FarmerApplication? existing}) async {
    final result = await showDialog<FarmerApplication>(
      context: context,
      builder: (_) => ApplicationDialog(existing: existing),
    );
    if (result == null) return;
    setState(() {
      final index = _records.indexWhere((r) => r.id == result.id);
      if (index >= 0) {
        _records[index] = result;
      } else {
        _records.insert(0, result);
      }
    });
    await _persist();
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(existing == null
            ? 'Farm profile saved on this device.'
            : 'Farm profile updated.'),
        behavior: SnackBarBehavior.floating,
      ),
    );
  }

  static const _reportTypes = [
    'Farm Profile', 'Crop Records', 'Livestock Records',
    'Finance Records', 'Task Register', 'Inventory Records',
    'Production Records', 'Equipment Register', 'Field Mapping Records',
    'Pest & Disease Log', 'Incident Register', 'Complete Farm Activity',
  ];

  List<Map<String, dynamic>> _reportRows() {
    final chosenRecords = _reportScope == 'Selected farm'
        ? _records.where((r) => r.id == (_selectedFarmRecord?.id ?? '')).toList()
        : _records;
    final chosenIds = chosenRecords.map((r) => r.id).toSet();
    final activities = _activities.where((a) => chosenIds.contains(a.farmRecordId)).toList();
    final selectedType = switch (_reportType) {
      'Crop Records' => 'Crops',
      'Livestock Records' => 'Livestock',
      'Finance Records' => 'Finance',
      'Task Register' => 'Tasks',
      'Inventory Records' => 'Inventory',
      'Production Records' => 'Production',
      'Equipment Register' => 'Equipment',
      'Field Mapping Records' => 'Field Mapping',
      'Pest & Disease Log' => 'Pest & Disease',
      'Incident Register' => 'Incidents',
      _ => '',
    };
    final chosenActivities = selectedType.isEmpty
        ? activities
        : activities.where((a) => a.type == selectedType).toList();

    if (_reportType == 'Farm Profile') {
      return chosenRecords.map((r) => <String, dynamic>{
        'Farm Record ID': r.id,
        'Farmer Name': r.farmerName,
        'Contact Number': r.contact,
        'Farm Name': r.farmName,
        'Province': r.province,
        'Municipality/City': r.municipality,
        'Barangay': r.barangay,
        'Farm Area': r.area,
        'Land Tenure': r.landTenure,
        'Commodities': r.commodities,
        'Last Updated': r.updatedAt,
      }).toList();
    }

    final farmerNames = {for (final r in _records) r.id: r.farmerName};
    return chosenActivities.map((a) => <String, dynamic>{
      'Farm Record ID': a.farmRecordId,
      'Farmer Name': farmerNames[a.farmRecordId] ?? 'Unknown farmer',
      'Record ID': a.id,
      'Record Type': a.type,
      'Title': a.title,
      'Category': a.category,
      'Quantity': a.quantity,
      'Unit': a.unit,
      'Amount': a.amount,
      'Date': a.date,
      'Status': a.status,
      'Notes': a.notes,
      'Last Updated': a.updatedAt,
    }).toList();
  }

  String _rowsToCsv(List<Map<String, dynamic>> rows) {
    if (_reportType == 'Farm Profile') {
      final text = farmProfilesToCsv(_records.where((r) =>
          _reportScope == 'All my farms' || r.id == (_selectedFarmRecord?.id ?? '')).toList());
      return text;
    }
    const columns = [
      'Farm Record ID', 'Farmer Name', 'Record ID', 'Record Type', 'Title',
      'Category', 'Quantity', 'Unit', 'Amount', 'Date', 'Status', 'Notes', 'Last Updated',
    ];
    String cell(String value) {
      // Escape spreadsheet formula prefixes, including those after leading whitespace.
      final dangerous = RegExp(r'^[\s\uFEFF]*[=+\-@＝＋－＠]').hasMatch(value);
      final safe = dangerous ? "'$value" : value;
      return '"${safe.replaceAll('"', '""')}"';
    }
    final outputRows = <List<String>>[
      columns,
      ...rows.map((row) => columns.map((column) => (row[column] ?? '').toString()).toList()),
    ];
    return '\uFEFF${outputRows.map((row) => row.map(cell).join(',')).join('\r\n')}';
  }

  Future<void> _exportReport() async {
    final rows = _reportRows();
    if (rows.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Walang farm records para sa napiling report.')),
      );
      return;
    }
    final now = DateTime.now();
    final extension = _reportFormat.toLowerCase();
    final safeType = _reportType.toLowerCase().replaceAll(RegExp(r'[^a-z0-9]+'), '-');
    final fileName = 'agrivision-$safeType-${now.toIso8601String().substring(0, 10)}.$extension';
    final List<int> fileBytes;
    final String mimeType;

    if (_reportFormat == 'PDF') {
      final document = pw.Document();
      final columns = rows.first.keys.toList();
      document.addPage(
        pw.MultiPage(
          pageFormat: PdfPageFormat.a4,
          margin: const pw.EdgeInsets.all(28),
          build: (context) => [
            pw.Text(
              'AgriVision · $_reportType',
              style: pw.TextStyle(fontSize: 18, fontWeight: pw.FontWeight.bold),
            ),
            pw.SizedBox(height: 5),
            pw.Text('Scope: $_reportScope'),
            pw.Text('Generated on device: ${now.toIso8601String()}'),
            pw.SizedBox(height: 14),
            pw.TableHelper.fromTextArray(
              headers: columns,
              data: rows.map((row) =>
                  columns.map((column) => (row[column] ?? '').toString()).toList()
              ).toList(),
              headerDecoration: pw.BoxDecoration(color: PdfColor.fromHex('#173F35')),
              headerStyle: pw.TextStyle(
                color: PdfColors.white,
                fontWeight: pw.FontWeight.bold,
                fontSize: 8,
              ),
              cellStyle: const pw.TextStyle(fontSize: 7),
              cellAlignment: pw.Alignment.centerLeft,
              border: pw.TableBorder.all(color: PdfColors.grey300, width: 0.35),
            ),
            pw.SizedBox(height: 10),
            pw.Text(
              'Offline report generated from records saved on this device.',
              style: pw.TextStyle(fontSize: 8, color: PdfColors.grey700),
            ),
          ],
        ),
      );
      fileBytes = await document.save();
      mimeType = 'application/pdf';
    } else {
      final content = _reportFormat == 'JSON'
          ? const JsonEncoder.withIndent('  ').convert({
              'app': 'AgriVision Flutter',
              'reportType': _reportType,
              'scope': _reportScope,
              'generatedAt': now.toIso8601String(),
              'recordCount': rows.length,
              'records': rows,
            })
          : _rowsToCsv(rows);
      fileBytes = utf8.encode(content);
      mimeType = _reportFormat == 'JSON' ? 'application/json' : 'text/csv';
    }

    await SharePlus.instance.share(
      ShareParams(
        files: [XFile.fromData(Uint8List.fromList(fileBytes), mimeType: mimeType)],
        subject: 'AgriVision $_reportType $_reportFormat',
        fileNameOverrides: [fileName],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final pages = [
      _dashboard(),
      _registry(),
      _farmRecords(),
      _reports(),
      _more(),
    ];
    return Scaffold(
      body: SafeArea(
        child: _loading
            ? const Center(child: CircularProgressIndicator(color: _green))
            : IndexedStack(index: _tab, children: pages),
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _tab,
        onDestinationSelected: (value) => setState(() => _tab = value),
        backgroundColor: Colors.white,
        indicatorColor: _mint,
        destinations: const [
          NavigationDestination(icon: Icon(Icons.home_rounded), label: 'Home'),
          NavigationDestination(icon: Icon(Icons.agriculture_outlined), label: 'My Farms'),
          NavigationDestination(icon: Icon(Icons.receipt_long_outlined), label: 'Farm Log'),
          NavigationDestination(icon: Icon(Icons.assessment_outlined), label: 'My Reports'),
          NavigationDestination(icon: Icon(Icons.tune_rounded), label: 'More'),
        ],
      ),
    );
  }

  Widget _dashboard() {
    final profile = _selectedFarmRecord;
    final ownActivities = profile == null
        ? <FarmActivity>[]
        : (_activities.where((a) => a.farmRecordId == profile.id).toList()
          ..sort((a, b) => b.updatedAt.compareTo(a.updatedAt)));
    final crops = ownActivities.where((a) => a.type == 'Crops').length;
    final livestock = ownActivities.where((a) => a.type == 'Livestock').length;
    final openTasks = ownActivities.where((a) {
      if (a.type != 'Tasks') return false;
      final status = a.status.toLowerCase();
      return status != 'completed' && status != 'cancelled' && status != 'done';
    }).length;
    final profileSubtitle = profile == null
        ? 'Set up your profile to start keeping farm records.'
        : (profile.farmName.trim().isEmpty ? 'Your farm profile' : profile.farmName);

    return CustomScrollView(
      slivers: [
        SliverPadding(
          padding: const EdgeInsets.fromLTRB(20, 14, 20, 28),
          sliver: SliverList(
            delegate: SliverChildListDelegate([
              _topBar(),
              const SizedBox(height: 22),
              Container(
                padding: const EdgeInsets.all(22),
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(28),
                  gradient: const LinearGradient(
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                    colors: [_forest, Color(0xFF2F7559), Color(0xFF4C9270)],
                  ),
                ),
                child: Stack(
                  children: [
                    Positioned(
                      right: -10,
                      top: -15,
                      child: Opacity(
                        opacity: .10,
                        child: const AgriVisionMark(
                          size: 130,
                          backgroundColor: Colors.transparent,
                          onDark: true,
                        ),
                      ),
                    ),
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                          decoration: BoxDecoration(
                            color: Colors.white.withValues(alpha: .14),
                            borderRadius: BorderRadius.circular(30),
                          ),
                          child: const Text('MY FARM',
                              style: TextStyle(color: Colors.white, fontSize: 10,
                                  fontWeight: FontWeight.w800, letterSpacing: 1.1)),
                        ),
                        const SizedBox(height: 18),
                        const Text('Grow your farm,\none step at a time.',
                            style: TextStyle(color: Colors.white, fontSize: 27,
                                height: 1.12, fontWeight: FontWeight.w800)),
                        const SizedBox(height: 10),
                        Text(
                          'Keep your farm profile, crops, livestock, finances, and daily tasks organized in one place.',
                          style: TextStyle(color: Colors.white.withValues(alpha: .82),
                              height: 1.45, fontSize: 13),
                        ),
                        const SizedBox(height: 20),
                        FilledButton.icon(
                          onPressed: () => _openApplication(existing: profile),
                          style: FilledButton.styleFrom(
                            backgroundColor: Colors.white,
                            foregroundColor: _forest,
                            padding: const EdgeInsets.symmetric(horizontal: 15, vertical: 13),
                          ),
                          icon: Icon(profile == null ? Icons.add_home_work_outlined : Icons.edit_outlined, size: 18),
                          label: Text(profile == null ? 'Set up my farm' : 'Edit my farm profile'),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 24),
              _SectionHeading(title: 'My farm at a glance', subtitle: profileSubtitle),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(child: _MetricCard(
                    icon: Icons.grass_outlined, label: 'Crop entries',
                    value: crops.toString(), tint: _mint, iconColor: _green,
                  )),
                  const SizedBox(width: 10),
                  Expanded(child: _MetricCard(
                    icon: Icons.pets_outlined, label: 'Livestock',
                    value: livestock.toString(), tint: const Color(0xFFE6ECFF),
                    iconColor: const Color(0xFF4C62A8),
                  )),
                  const SizedBox(width: 10),
                  Expanded(child: _MetricCard(
                    icon: Icons.checklist_rounded, label: 'Open tasks',
                    value: openTasks.toString(), tint: const Color(0xFFFFF0D7), iconColor: _amber,
                  )),
                ],
              ),
              const SizedBox(height: 24),
              Row(
                children: [
                  const Expanded(child: _SectionHeading(title: 'My farm profiles', subtitle: 'Your saved farm details')),
                  TextButton(
                    onPressed: () => setState(() => _tab = 1),
                    child: const Text('Manage'),
                  ),
                ],
              ),
              const SizedBox(height: 10),
              if (_records.isEmpty)
                _EmptyCard(
                  icon: Icons.agriculture_outlined,
                  title: 'Set up your farm',
                  message: 'Add your details to start recording crops, livestock, expenses, and tasks.',
                  action: FilledButton.icon(
                    onPressed: () => _openApplication(),
                    icon: const Icon(Icons.add_home_work_outlined),
                    label: const Text('Set up my farm'),
                  ),
                )
              else
                ..._records.take(3).map((r) => Padding(
                  padding: const EdgeInsets.only(bottom: 10),
                  child: _FarmerTile(
                    record: r,
                    onTap: () => _openApplication(existing: r),
                  ),
                )),
              const SizedBox(height: 10),
              _ReportShortcut(onTap: () => setState(() => _tab = 3)),
            ]),
          ),
        ),
      ],
    );
  }

  Widget _registry() {
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(20, 18, 20, 12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _topBar(),
              const SizedBox(height: 18),
              const _SectionHeading(
                title: 'My Farm Profiles',
                subtitle: 'Manage your details and the farms you keep records for.',
              ),
              const SizedBox(height: 14),
              TextField(
                onChanged: (value) => setState(() => _search = value),
                decoration: const InputDecoration(
                  hintText: 'Search your farm, name, or location',
                  prefixIcon: Icon(Icons.search_rounded),
                ),
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Text('${_filteredRecords.length} farm profiles',
                      style: const TextStyle(color: _muted, fontWeight: FontWeight.w600)),
                  const Spacer(),
                  FilledButton.icon(
                    onPressed: () => _openApplication(),
                    icon: const Icon(Icons.add_rounded, size: 18),
                    label: const Text('Add farm'),
                  ),
                ],
              ),
            ],
          ),
        ),
        Expanded(
          child: _filteredRecords.isEmpty
              ? _EmptyCard(
                  icon: _search.isEmpty ? Icons.agriculture_outlined : Icons.search_off_rounded,
                  title: _search.isEmpty ? 'No farm profile yet' : 'No matching farm profiles',
                  message: _search.isEmpty
                      ? 'Set up your farmer details and farm information to begin.'
                      : 'Try another farm name, your name, or location.',
                  action: _search.isEmpty
                      ? FilledButton.icon(
                          onPressed: () => _openApplication(),
                          icon: const Icon(Icons.add_home_work_outlined),
                          label: const Text('Set up my farm'),
                        )
                      : null,
                )
              : ListView.separated(
                  padding: const EdgeInsets.fromLTRB(20, 4, 20, 24),
                  itemCount: _filteredRecords.length,
                  separatorBuilder: (context, index) => const SizedBox(height: 10),
                  itemBuilder: (context, index) {
                    final record = _filteredRecords[index];
                    return _FarmerTile(
                      record: record,
                      onTap: () => _openApplication(existing: record),
                    );
                  },
                ),
        ),
      ],
    );
  }

  Future<void> _openActivityDialog({
    required String farmRecordId,
    required String type,
    FarmActivity? existing,
  }) async {
    final result = await showDialog<FarmActivity>(
      context: context,
      builder: (_) => ActivityDialog(
        farmRecordId: farmRecordId,
        type: type,
        existing: existing,
      ),
    );
    if (result == null) return;
    setState(() {
      final index = _activities.indexWhere((a) => a.id == result.id);
      if (index >= 0) {
        _activities[index] = result;
      } else {
        _activities.insert(0, result);
      }
    });
    await _persistActivities();
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(existing == null ? '$type record saved.' : '$type record updated.'),
        behavior: SnackBarBehavior.floating,
      ),
    );
  }

  Future<void> _deleteActivity(FarmActivity activity) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Delete farm record?'),
        content: Text('Delete "${activity.title}" from this farmer’s records?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancel')),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            style: FilledButton.styleFrom(backgroundColor: const Color(0xFFB33B32)),
            child: const Text('Delete'),
          ),
        ],
      ),
    );
    if (confirmed != true) return;
    setState(() => _activities.removeWhere((a) => a.id == activity.id));
    await _persistActivities();
  }

  Widget _farmRecords() {
    final chosen = _selectedFarmRecord;
    final selectedId = chosen?.id ?? '';
    final linkedActivities = _activities.where((a) => a.farmRecordId == selectedId).toList();
    final categoryCount = linkedActivities.where((a) => a.type == _activityType).length;
    final visibleActivities = _filteredActivities;
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(20, 18, 20, 12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _topBar(),
              const SizedBox(height: 18),
              const _SectionHeading(
                title: 'My Farm Log',
                subtitle: 'Track crops, livestock, farm finances, and daily tasks in your own farm log.',
              ),
              const SizedBox(height: 14),
              if (_records.isNotEmpty)
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 4),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(15),
                    border: Border.all(color: _line),
                  ),
                  child: DropdownButtonHideUnderline(
                    child: DropdownButton<String>(
                      isExpanded: true,
                      value: selectedId,
                      icon: const Icon(Icons.expand_more_rounded),
                      items: _records.map((r) => DropdownMenuItem<String>(
                        value: r.id,
                        child: Text(
                          '${r.farmerName} · ${r.farmName.isEmpty ? 'Farm not specified' : r.farmName}',
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      )).toList(),
                      onChanged: (value) => setState(() => _selectedFarmRecordId = value ?? ''),
                    ),
                  ),
                ),
              const SizedBox(height: 12),
              SingleChildScrollView(
                scrollDirection: Axis.horizontal,
                child: Row(
                  children: [
                    _activityChip('Crops', Icons.grass_outlined),
                    const SizedBox(width: 7),
                    _activityChip('Livestock', Icons.pets_outlined),
                    const SizedBox(width: 7),
                    _activityChip('Finance', Icons.account_balance_wallet_outlined),
                    const SizedBox(width: 7),
                    _activityChip('Tasks', Icons.checklist_rounded),
                    const SizedBox(width: 7),
                    _activityChip('Inventory', Icons.inventory_2_outlined),
                    const SizedBox(width: 7),
                    _activityChip('Production', Icons.agriculture_rounded),
                    const SizedBox(width: 7),
                    _activityChip('Equipment', Icons.handyman_outlined),
                    const SizedBox(width: 7),
                    _activityChip('Field Mapping', Icons.map_outlined),
                    const SizedBox(width: 7),
                    _activityChip('Pest & Disease', Icons.biotech_outlined),
                    const SizedBox(width: 7),
                    _activityChip('Incidents', Icons.report_gmailerrorred_outlined),
                  ],
                ),
              ),
              const SizedBox(height: 12),
              if (_records.isNotEmpty)
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        onChanged: (value) => setState(() => _activitySearch = value),
                        decoration: const InputDecoration(
                          hintText: 'Search records',
                          prefixIcon: Icon(Icons.search_rounded),
                          isDense: true,
                        ),
                      ),
                    ),
                    const SizedBox(width: 9),
                    FilledButton.icon(
                      onPressed: () => _openActivityDialog(farmRecordId: selectedId, type: _activityType),
                      icon: const Icon(Icons.add_rounded, size: 18),
                      label: const Text('Add'),
                    ),
                  ],
                ),
              if (chosen != null) ...[
                const SizedBox(height: 11),
                Row(
                  children: [
                    Expanded(
                      child: Text(
                        '${chosen.farmerName} · ${chosen.id}',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(color: _muted, fontSize: 11, fontWeight: FontWeight.w700),
                      ),
                    ),
                    Text('$categoryCount ${_activityType.toLowerCase()} records',
                        style: const TextStyle(color: _green, fontSize: 11, fontWeight: FontWeight.w800)),
                  ],
                ),
              ],
            ],
          ),
        ),
        Expanded(
          child: _records.isEmpty
              ? _EmptyCard(
                  icon: Icons.agriculture_outlined,
                  title: 'Set up your farm first',
                  message: 'Add your farm details before recording crops, livestock, finances, or daily tasks.',
                  action: FilledButton.icon(
                    onPressed: () => _openApplication(),
                    icon: const Icon(Icons.person_add_alt_1_rounded),
                    label: const Text('Set up my farm'),
                  ),
                )
              : visibleActivities.isEmpty
                  ? _EmptyCard(
                      icon: _activityIcon(_activityType),
                      title: 'No ${_activityType.toLowerCase()} records yet',
                      message: 'Add a ${_activityType.toLowerCase()} record for ${chosen?.farmerName ?? 'this farmer'}. The record will stay linked to this farmer.',
                      action: FilledButton.icon(
                        onPressed: () => _openActivityDialog(farmRecordId: selectedId, type: _activityType),
                        icon: const Icon(Icons.add_rounded),
                        label: Text('Add ${_activityType.toLowerCase()} record'),
                      ),
                    )
                  : ListView.separated(
                      padding: const EdgeInsets.fromLTRB(20, 2, 20, 24),
                      itemCount: visibleActivities.length,
                      separatorBuilder: (context, index) => const SizedBox(height: 9),
                      itemBuilder: (context, index) {
                        final activity = visibleActivities[index];
                        return _ActivityTile(
                          activity: activity,
                          onEdit: () => _openActivityDialog(
                            farmRecordId: activity.farmRecordId,
                            type: activity.type,
                            existing: activity,
                          ),
                          onDelete: () => _deleteActivity(activity),
                        );
                      },
                    ),
        ),
      ],
    );
  }

  IconData _activityIcon(String type) => switch (type) {
    'Crops' => Icons.grass_outlined,
    'Livestock' => Icons.pets_outlined,
    'Finance' => Icons.account_balance_wallet_outlined,
    'Tasks' => Icons.checklist_rounded,
    'Inventory' => Icons.inventory_2_outlined,
    'Production' => Icons.agriculture_rounded,
    'Equipment' => Icons.handyman_outlined,
    'Field Mapping' => Icons.map_outlined,
    'Pest & Disease' => Icons.biotech_outlined,
    'Incidents' => Icons.report_gmailerrorred_outlined,
    _ => Icons.folder_outlined,
  };

  Widget _activityChip(String type, IconData icon) => FilterChip(
    selected: _activityType == type,
    onSelected: (_) => setState(() => _activityType = type),
    avatar: Icon(icon, size: 17, color: _activityType == type ? _green : _muted),
    label: Text(type),
    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(13)),
  );

  Widget _reports() {
    final reportCount = _reportRows().length;
    final profile = _selectedFarmRecord;
    final selectedName = profile == null
        ? 'No farm selected'
        : (profile.farmName.trim().isEmpty ? profile.farmerName : profile.farmName);
    final selectedIds = _reportScope == 'Selected farm'
        ? <String>{profile?.id ?? ''}
        : _records.map((r) => r.id).toSet();
    final selectedActivities = _activities.where((a) => selectedIds.contains(a.farmRecordId));
    final cropCount = selectedActivities.where((a) => a.type == 'Crops').length;
    final livestockCount = selectedActivities.where((a) => a.type == 'Livestock').length;

    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 18, 20, 28),
      children: [
        _topBar(),
        const SizedBox(height: 22),
        const _SectionHeading(
          title: 'My Reports',
          subtitle: 'Export your farm profile and activity records as CSV or JSON files.',
        ),
        const SizedBox(height: 18),
        Container(
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            color: _forest,
            borderRadius: BorderRadius.circular(24),
            gradient: const LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [_forest, Color(0xFF2F7559)],
            ),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Icon(Icons.description_outlined, color: Colors.white, size: 30),
              const SizedBox(height: 15),
              const Text(
                'Farm Report Builder',
                style: TextStyle(color: Colors.white, fontSize: 21, fontWeight: FontWeight.w900),
              ),
              const SizedBox(height: 6),
              Text(
                'Prepare a farm report for ${_reportScope == 'Selected farm' ? selectedName : 'all your farm profiles'}.',
                style: TextStyle(color: Colors.white.withValues(alpha: .8), height: 1.45),
              ),
              const SizedBox(height: 18),
              const Text('What to export', style: TextStyle(color: Colors.white, fontWeight: FontWeight.w700)),
              const SizedBox(height: 7),
              _darkSelector<String>(
                value: _reportType,
                values: _reportTypes,
                label: (value) => value,
                onChanged: (value) => setState(() => _reportType = value),
              ),
              const SizedBox(height: 14),
              const Text('Farm selection', style: TextStyle(color: Colors.white, fontWeight: FontWeight.w700)),
              const SizedBox(height: 7),
              _darkSelector<String>(
                value: _reportScope,
                values: const ['All my farms', 'Selected farm'],
                label: (value) => value,
                onChanged: (value) => setState(() => _reportScope = value),
              ),
              const SizedBox(height: 14),
              const Text('File format', style: TextStyle(color: Colors.white, fontWeight: FontWeight.w700)),
              const SizedBox(height: 7),
              Row(
                children: [
                  Expanded(child: _formatChoice('CSV')),
                  const SizedBox(width: 9),
                  Expanded(child: _formatChoice('JSON')),
                  const SizedBox(width: 9),
                  Expanded(child: _formatChoice('PDF')),
                ],
              ),
              const SizedBox(height: 16),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(13),
                decoration: BoxDecoration(
                  color: Colors.white.withValues(alpha: .1),
                  borderRadius: BorderRadius.circular(15),
                  border: Border.all(color: Colors.white.withValues(alpha: .14)),
                ),
                child: Row(
                  children: [
                    const Icon(Icons.fact_check_outlined, color: Color(0xFFC8E9A8)),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('$reportCount records',
                              style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w900, fontSize: 17)),
                          Text('$_reportType · $_reportFormat · $_reportScope',
                              style: TextStyle(color: Colors.white.withValues(alpha: .77), fontSize: 11)),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 15),
              FilledButton.icon(
                onPressed: reportCount == 0 ? null : _exportReport,
                style: FilledButton.styleFrom(
                  backgroundColor: Colors.white,
                  foregroundColor: _forest,
                  disabledBackgroundColor: Colors.white.withValues(alpha: .35),
                  minimumSize: const Size.fromHeight(49),
                ),
                icon: const Icon(Icons.ios_share_rounded),
                label: Text(reportCount == 0 ? 'No records to export' : 'Share $_reportFormat report'),
              ),
            ],
          ),
        ),
        const SizedBox(height: 18),
        Row(
          children: [
            Expanded(child: _MetricCard(
              icon: Icons.home_work_outlined, label: 'Farm profiles',
              value: (_reportScope == 'Selected farm'
                  ? (profile == null ? 0 : 1)
                  : _records.length).toString(),
              tint: _mint, iconColor: _green,
            )),
            const SizedBox(width: 10),
            Expanded(child: _MetricCard(
              icon: Icons.grass_outlined, label: 'Crop entries',
              value: cropCount.toString(), tint: const Color(0xFFFFF0D7), iconColor: _amber,
            )),
            const SizedBox(width: 10),
            Expanded(child: _MetricCard(
              icon: Icons.pets_outlined, label: 'Livestock entries',
              value: livestockCount.toString(), tint: const Color(0xFFE6ECFF),
              iconColor: const Color(0xFF4C62A8),
            )),
          ],
        ),
        const SizedBox(height: 18),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(17),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Included information', style: TextStyle(fontWeight: FontWeight.w800, fontSize: 16)),
                const SizedBox(height: 5),
                Text(
                  _reportType == 'Farm Profile'
                      ? 'Your farmer and farm details'
                      : 'Your farm-linked ${_reportType.toLowerCase()} entries',
                  style: const TextStyle(color: _muted, fontSize: 12),
                ),
                const SizedBox(height: 12),
                ..._reportIncludedFields().map((text) => Padding(
                  padding: const EdgeInsets.only(bottom: 9),
                  child: Row(
                    children: [
                      const Icon(Icons.check_circle_outline_rounded, color: _green, size: 19),
                      const SizedBox(width: 9),
                      Expanded(child: Text(text, style: const TextStyle(color: _muted))),
                    ],
                  ),
                )),
              ],
            ),
          ),
        ),
        const SizedBox(height: 12),
        const Text(
          'Exports contain only records saved in this app on this device. Nothing is automatically uploaded or synced to a central server.',
          style: TextStyle(color: _muted, fontSize: 12, height: 1.5),
        ),
      ],
    );
  }

  Widget _darkSelector<T>({
    required T value,
    required List<T> values,
    required String Function(T) label,
    required ValueChanged<T> onChanged,
  }) => Container(
    padding: const EdgeInsets.symmetric(horizontal: 12),
    decoration: BoxDecoration(
      color: Colors.white,
      borderRadius: BorderRadius.circular(13),
    ),
    child: DropdownButtonHideUnderline(
      child: DropdownButton<T>(
        value: values.contains(value) ? value : values.first,
        isExpanded: true,
        borderRadius: BorderRadius.circular(13),
        items: values.map((item) => DropdownMenuItem<T>(
          value: item,
          child: Text(label(item), maxLines: 1, overflow: TextOverflow.ellipsis),
        )).toList(),
        onChanged: (next) { if (next != null) onChanged(next); },
      ),
    ),
  );

  Widget _formatChoice(String format) => InkWell(
    onTap: () => setState(() => _reportFormat = format),
    borderRadius: BorderRadius.circular(12),
    child: Container(
      padding: const EdgeInsets.symmetric(vertical: 12),
      decoration: BoxDecoration(
        color: _reportFormat == format ? Colors.white : Colors.white.withValues(alpha: .09),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.white.withValues(alpha: .2)),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(
            _reportFormat == format ? Icons.radio_button_checked_rounded : Icons.radio_button_unchecked_rounded,
            size: 17,
            color: _reportFormat == format ? _green : Colors.white,
          ),
          const SizedBox(width: 7),
          Text(format, style: TextStyle(
            color: _reportFormat == format ? _forest : Colors.white,
            fontWeight: FontWeight.w800,
          )),
        ],
      ),
    ),
  );

  List<String> _reportIncludedFields() {
    if (_reportType == 'Farm Profile') {
      return const [
        'Farmer/farm record IDs, farmer name, and contact number',
        'Farm name, location, area, land tenure, and commodities',
        'Farmer name, contact details, farm location, and last-updated date',
      ];
    }
    return const [
      'Farmer name and linked farm record ID',
      'Record type, title, category, date, and status',
      'Quantity, unit, amount, and notes where applicable',
    ];
  }

  Future<void> _exportFullBackup() async {
    final content = buildAgriVisionBackupJson(
      farmers: _records,
      activities: _activities,
    );
    final now = DateTime.now();
    final fileName = 'agrivision-backup-${now.toIso8601String().substring(0, 10)}.json';
    try {
      await SharePlus.instance.share(
        ShareParams(
          files: [
            XFile.fromData(
              utf8.encode(content),
              mimeType: 'application/json',
            ),
          ],
          subject: 'AgriVision full local backup',
          fileNameOverrides: [fileName],
        ),
      );
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Hindi na-share ang backup. Subukan ulit.')),
      );
    }
  }

  Future<void> _restoreFullBackup() async {
    try {
      final pickedFiles = await FilePicker.pickFiles(
        type: FileType.custom,
        allowedExtensions: ['json'],
      );
      if (pickedFiles.isEmpty) return;

      final bytes = await pickedFiles.first.readAsBytes();
      if (bytes.length > 10 * 1024 * 1024) {
        throw const FormatException('Backup is larger than the 10 MB safety limit.');
      }
      final backup = parseAgriVisionBackupJson(utf8.decode(bytes));

      if (!mounted) return;
      final confirmed = await showDialog<bool>(
        context: context,
        builder: (dialogContext) => AlertDialog(
          title: const Text('Restore AgriVision backup?'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text('Backup contents', style: TextStyle(fontWeight: FontWeight.w800)),
              const SizedBox(height: 12),
              _backupCountRow(Icons.groups_2_outlined, 'Farmer records', backup.farmers.length),
              const SizedBox(height: 8),
              _backupCountRow(Icons.agriculture_outlined, 'Farm activity records', backup.activities.length),
              const SizedBox(height: 14),
              const Text(
                'Restore uses a safe merge: records with matching IDs are updated, new records are added, and local-only records are kept. Nothing is deleted.',
                style: TextStyle(fontSize: 12, height: 1.45, color: _muted),
              ),
              if (backup.exportedAt.isNotEmpty) ...[
                const SizedBox(height: 10),
                Text(
                  'Backup created: ${backup.exportedAt}',
                  style: const TextStyle(fontSize: 11, color: _muted),
                ),
              ],
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(dialogContext, false),
              child: const Text('Cancel'),
            ),
            FilledButton.icon(
              onPressed: () => Navigator.pop(dialogContext, true),
              icon: const Icon(Icons.restore_rounded, size: 18),
              label: const Text('Restore records'),
            ),
          ],
        ),
      );
      if (confirmed != true || !mounted) return;

      final mergedFarmers = <String, FarmerApplication>{
        for (final record in _records) record.id: record,
      };
      for (final record in backup.farmers) {
        mergedFarmers[record.id] = record;
      }
      final mergedActivities = <String, FarmActivity>{
        for (final activity in _activities) activity.id: activity,
      };
      for (final activity in backup.activities) {
        mergedActivities[activity.id] = activity;
      }

      final nextFarmers = mergedFarmers.values.toList();
      final nextActivities = mergedActivities.values.toList();
      final prefs = await SharedPreferences.getInstance();
      const farmersKey = 'agrivision_flutter_registry_v1';
      const activitiesKey = 'agrivision_flutter_operations_v1';
      final oldFarmers = prefs.getString(farmersKey);
      final oldActivities = prefs.getString(activitiesKey);
      try {
        final savedFarmers = await prefs.setString(
          farmersKey,
          jsonEncode(nextFarmers.map((record) => record.toJson()).toList()),
        );
        if (!savedFarmers) throw StateError('Unable to save farmer records.');
        final savedActivities = await prefs.setString(
          activitiesKey,
          jsonEncode(nextActivities.map((activity) => activity.toJson()).toList()),
        );
        if (!savedActivities) throw StateError('Unable to save farm activities.');
      } catch (_) {
        if (oldFarmers == null) {
          await prefs.remove(farmersKey);
        } else {
          await prefs.setString(farmersKey, oldFarmers);
        }
        if (oldActivities == null) {
          await prefs.remove(activitiesKey);
        } else {
          await prefs.setString(activitiesKey, oldActivities);
        }
        rethrow;
      }

      if (!mounted) return;
      setState(() {
        _records = nextFarmers;
        _activities = nextActivities;
      });
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            'Backup restored: ${backup.farmers.length} farmers and ${backup.activities.length} farm activity records imported.',
          ),
          behavior: SnackBarBehavior.floating,
        ),
      );
    } on FormatException catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Hindi ma-restore ang backup: ${error.message}')),
      );
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Hindi na-restore ang backup. Walang binagong records.')),
      );
    }
  }

  Widget _backupCountRow(IconData icon, String label, int count) => Row(
    children: [
      Icon(icon, color: _green, size: 19),
      const SizedBox(width: 9),
      Expanded(child: Text(label, style: const TextStyle(fontSize: 13))),
      Text(count.toString(), style: const TextStyle(fontWeight: FontWeight.w900, color: _forest)),
    ],
  );

  Widget _backupRestoreCard() => Card(
    child: Padding(
      padding: const EdgeInsets.all(17),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: 46,
                height: 46,
                decoration: BoxDecoration(color: _mint, borderRadius: BorderRadius.circular(14)),
                child: const Icon(Icons.backup_outlined, color: _green, size: 25),
              ),
              const SizedBox(width: 12),
              const Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('Backup & Restore', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w900)),
                    SizedBox(height: 4),
                    Text(
                      'Protect farmer registry and farm activity records when changing devices.',
                      style: TextStyle(color: _muted, fontSize: 12, height: 1.4),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          Container(
            padding: const EdgeInsets.all(11),
            decoration: BoxDecoration(
              color: const Color(0xFFF8FAF7),
              borderRadius: BorderRadius.circular(13),
              border: Border.all(color: _line),
            ),
            child: Row(
              children: [
                const Icon(Icons.storage_outlined, color: _green, size: 18),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    '${_records.length} farmers · ${_activities.length} activity records on this device',
                    style: const TextStyle(fontSize: 11, color: _muted, fontWeight: FontWeight.w700),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: _exportFullBackup,
                  icon: const Icon(Icons.ios_share_rounded, size: 18),
                  label: const Text('Export backup'),
                ),
              ),
              const SizedBox(width: 9),
              Expanded(
                child: FilledButton.icon(
                  onPressed: _restoreFullBackup,
                  icon: const Icon(Icons.restore_rounded, size: 18),
                  label: const Text('Restore'),
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          const Text(
            'Backup files contain farmer contact and farm data. Keep them in a private, trusted location. Cloud sync is not enabled.',
            style: TextStyle(fontSize: 11, color: _muted, height: 1.4),
          ),
        ],
      ),
    ),
  );

  Widget _offlineInsightsCard() {
    final selectedFarmId = _selectedFarmRecord?.id;
    final activities = _activities.where((row) =>
        selectedFarmId == null || row.farmRecordId == selectedFarmId).toList();
    final summary = calculateOfflineFarmSummary(activities);
    final todayKey = DateTime.now().toIso8601String().substring(0, 10);
    final dueTasks = activities.where((row) {
      if (row.type != 'Tasks') return false;
      final status = row.status.toLowerCase();
      return status != 'completed' && status != 'cancelled' &&
          row.date.compareTo(todayKey) <= 0;
    }).toList()
      ..sort((a, b) => a.date.compareTo(b.date));

    Widget metric(String title, String value, IconData icon) => Expanded(
      child: Container(
        padding: const EdgeInsets.all(11),
        decoration: BoxDecoration(color: _cream, borderRadius: BorderRadius.circular(14)),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(icon, color: _green, size: 19),
            const SizedBox(height: 8),
            Text(title, style: const TextStyle(color: _muted, fontSize: 10)),
            const SizedBox(height: 3),
            Text(value, maxLines: 1, overflow: TextOverflow.ellipsis,
                style: const TextStyle(color: _ink, fontSize: 15, fontWeight: FontWeight.w900)),
          ],
        ),
      ),
    );

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('Offline farm insights',
                style: TextStyle(fontSize: 17, fontWeight: FontWeight.w900, color: _ink)),
            const SizedBox(height: 4),
            const Text('Computed only from records stored on this phone.',
                style: TextStyle(color: _muted, fontSize: 12)),
            const SizedBox(height: 14),
            Row(
              children: [
                metric('Income logged', '\u{20B1}${summary.income.toStringAsFixed(0)}', Icons.trending_up_rounded),
                const SizedBox(width: 8),
                metric('Expenses logged', '\u{20B1}${summary.expenses.toStringAsFixed(0)}', Icons.trending_down_rounded),
                const SizedBox(width: 8),
                metric('Net recorded', '\u{20B1}${summary.netCashFlow.toStringAsFixed(0)}', Icons.account_balance_wallet_outlined),
              ],
            ),
            const SizedBox(height: 12),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                _insightPill('Overdue tasks: ${summary.overdueTasks}', summary.overdueTasks > 0),
                _insightPill('Due today: ${summary.tasksDueToday}', summary.tasksDueToday > 0),
                _insightPill('Production logs: ${summary.productionRecords}', false),
                _insightPill('Field notes: ${summary.fieldRecords}', false),
                _insightPill('Incidents: ${summary.incidentRecords}', summary.incidentRecords > 0),
                _insightPill('Inventory items: ${summary.inventoryItems}', false),
                _insightPill('Zero/negative stock: ${summary.lowOrEmptyStockItems}', summary.lowOrEmptyStockItems > 0),
              ],
            ),
            const SizedBox(height: 14),
            Row(
              children: [
                const Icon(Icons.notifications_active_outlined, color: _amber, size: 20),
                const SizedBox(width: 7),
                const Expanded(child: Text('Offline task reminders',
                    style: TextStyle(fontWeight: FontWeight.w800, color: _ink))),
                Text('${dueTasks.length} due',
                    style: const TextStyle(color: _muted, fontSize: 11)),
              ],
            ),
            const SizedBox(height: 5),
            if (dueTasks.isEmpty)
              const Text('Walang overdue o due-today na task sa saved records.',
                  style: TextStyle(color: _muted, fontSize: 12))
            else
              ...dueTasks.take(3).map((task) => ListTile(
                contentPadding: EdgeInsets.zero,
                dense: true,
                leading: Icon(
                  task.date.compareTo(todayKey) < 0
                      ? Icons.error_outline_rounded
                      : Icons.today_rounded,
                  color: task.date.compareTo(todayKey) < 0 ? const Color(0xFFB33B32) : _amber,
                ),
                title: Text(task.title, maxLines: 1, overflow: TextOverflow.ellipsis),
                subtitle: Text('${task.date} · ${task.status}'),
                trailing: IconButton(
                  tooltip: 'Open task',
                  icon: const Icon(Icons.chevron_right_rounded),
                  onPressed: () => setState(() {
                    _activityType = 'Tasks';
                    _tab = 2;
                    _selectedFarmRecordId = task.farmRecordId;
                    _activitySearch = task.title;
                  }),
                ),
              )),
            const SizedBox(height: 6),
            const Text(
              'Finance totals use records categorized as income/sales or expenses. Inventory balances use matching item names and stock-in/stock-out entries. Check entries before relying on totals.',
              style: TextStyle(color: _muted, fontSize: 10, height: 1.4),
            ),
          ],
        ),
      ),
    );
  }

  Widget _insightPill(String label, bool attention) => Container(
    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 7),
    decoration: BoxDecoration(
      color: attention ? const Color(0xFFFFF0D7) : _mint,
      borderRadius: BorderRadius.circular(11),
    ),
    child: Text(label, style: TextStyle(
      color: attention ? _amber : _forest,
      fontSize: 10,
      fontWeight: FontWeight.w800,
    )),
  );

  Widget _more() {
    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 18, 20, 28),
      children: [
        _topBar(),
        const SizedBox(height: 22),
        const _SectionHeading(title: 'More', subtitle: 'App information and data status'),
        const SizedBox(height: 16),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(18),
            child: Row(
              children: [
                Container(
                  width: 56,
                  height: 56,
                  decoration: BoxDecoration(color: _mint, borderRadius: BorderRadius.circular(17)),
                  child: const AgriVisionMark(size: 56, backgroundColor: _mint, onDark: false),
                ),
                const SizedBox(width: 14),
                const Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('AgriVision', style: TextStyle(fontSize: 20, fontWeight: FontWeight.w800)),
                      SizedBox(height: 3),
                      Text('Farmer Edition · Flutter', style: TextStyle(color: _muted, fontSize: 12)),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 12),
        _offlineInsightsCard(),
        const SizedBox(height: 12),
        _backupRestoreCard(),
        const SizedBox(height: 12),
        const _InfoTile(
          icon: Icons.offline_pin_outlined,
          title: 'Your farm data stays local',
          subtitle: 'Your farm profile and activity records are saved on this device.',
        ),
        const SizedBox(height: 10),
        const _InfoTile(
          icon: Icons.cloud_off_outlined,
          title: 'Cloud sync not connected',
          subtitle: 'Records stay on this device until you export or share a backup.',
        ),
        const SizedBox(height: 10),
        const _InfoTile(
          icon: Icons.verified_user_outlined,
          title: 'Farmer privacy',
          subtitle: 'Exported backups can contain your contact details and farm information. Store them safely.',
        ),
        const SizedBox(height: 20),
        OutlinedButton.icon(
          onPressed: () => setState(() => _tab = 0),
          icon: const Icon(Icons.home_outlined),
          label: const Text('Back to home'),
          style: OutlinedButton.styleFrom(minimumSize: const Size.fromHeight(48)),
        ),
      ],
    );
  }

  Widget _topBar() {
    return Row(
      children: [
        Container(
          width: 44,
          height: 44,
          decoration: BoxDecoration(color: _forest, borderRadius: BorderRadius.circular(14)),
          child: const AgriVisionMark(size: 44, backgroundColor: _forest, onDark: true),
        ),
        const SizedBox(width: 11),
        const Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('AgriVision', style: TextStyle(fontSize: 20, fontWeight: FontWeight.w900, letterSpacing: -.4)),
              Text('FARMER EDITION', style: TextStyle(fontSize: 9, color: _muted,
                  fontWeight: FontWeight.w800, letterSpacing: 1.4)),
            ],
          ),
        ),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
          decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(13),
              border: Border.all(color: _line)),
          child: Row(
            children: [
              const Icon(Icons.storage_rounded, color: _green, size: 15),
              const SizedBox(width: 6),
              Text('${_records.length} saved', style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w700)),
            ],
          ),
        ),
      ],
    );
  }
}

class ActivityDialog extends StatefulWidget {
  const ActivityDialog({
    super.key,
    required this.farmRecordId,
    required this.type,
    this.existing,
  });

  final String farmRecordId;
  final String type;
  final FarmActivity? existing;

  @override
  State<ActivityDialog> createState() => _ActivityDialogState();
}

class _ActivityDialogState extends State<ActivityDialog> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _title;
  late final TextEditingController _quantity;
  late final TextEditingController _unit;
  late final TextEditingController _amount;
  late final TextEditingController _date;
  late final TextEditingController _notes;
  late String _category;
  late String _status;

  List<String> get _categories => switch (widget.type) {
    'Crops' => const [
        'Land preparation', 'Planting', 'Irrigation', 'Fertilization',
        'Pest / disease monitoring', 'Harvest', 'Other',
      ],
    'Livestock' => const [
        'Animal group', 'Feed', 'Health / medicine', 'Mortality',
        'Production', 'Sales', 'Other',
      ],
    'Finance' => const ['Expense', 'Income / sale', 'Other income', 'Other'],
    'Tasks' => const [
        'Farm maintenance', 'Planting schedule', 'Watering', 'Fertilizing',
        'Spraying', 'Harvest schedule', 'Follow-up', 'Other',
      ],
    'Inventory' => const [
        'Stock In / purchase', 'Stock Out / used', 'Wastage',
        'Stock count / adjustment', 'Expiry check', 'Other',
      ],
    'Production' => const ['Harvest', 'Eggs', 'Milk', 'Livestock growth', 'Other'],
    'Equipment' => const ['Purchase', 'Maintenance', 'Repair', 'Fuel', 'Other'],
    'Field Mapping' => const [
        'Farm location', 'Field point / coordinate', 'Boundary point',
        'Boundary notes', 'Other',
      ],
    'Pest & Disease' => const [
        'Observation', 'Suspected pest', 'Suspected disease',
        'Treatment applied', 'Follow-up', 'Other',
      ],
    'Incidents' => const [
        'Weather damage', 'Pest / disease outbreak', 'Livestock health incident',
        'Crop damage', 'Equipment damage', 'Loss / theft', 'Other',
      ],
    _ => const ['Farm activity', 'Other'],
  };

  List<String> get _statuses => switch (widget.type) {
    'Crops' => const ['Planned', 'In progress', 'Harvested', 'Needs attention', 'Completed'],
    'Livestock' => const ['Active', 'For monitoring', 'Needs attention', 'Completed'],
    'Finance' => const ['Recorded', 'Pending', 'Paid', 'Received'],
    'Tasks' => const ['Pending', 'In progress', 'Completed', 'Cancelled'],
    'Inventory' => const ['In stock', 'Low stock', 'Out of stock', 'Expired', 'Disposed'],
    'Production' => const ['Recorded', 'Verified', 'Sold', 'Spoiled'],
    'Equipment' => const ['Available', 'In use', 'Under maintenance', 'Needs repair', 'Retired'],
    'Field Mapping' => const ['Draft', 'Recorded', 'Verified', 'Needs update'],
    'Pest & Disease' => const ['Observed', 'Monitoring', 'Treatment applied', 'Resolved', 'Needs expert review'],
    'Incidents' => const ['Draft', 'Recorded', 'Needs follow-up', 'Resolved'],
    _ => const ['Pending', 'In progress', 'Completed', 'Cancelled'],
  };

  @override
  void initState() {
    super.initState();
    final activity = widget.existing;
    _title = TextEditingController(text: activity?.title ?? '');
    _quantity = TextEditingController(text: activity?.quantity ?? '');
    _unit = TextEditingController(text: activity?.unit ?? (
        widget.type == 'Crops' ? 'ha' :
        widget.type == 'Field Mapping' ? 'points' : ''
      ));
    _amount = TextEditingController(
      text: activity == null || activity.amount == 0 ? '' : activity.amount.toString(),
    );
    _date = TextEditingController(
      text: activity?.date ?? DateTime.now().toIso8601String().substring(0, 10),
    );
    _notes = TextEditingController(text: activity?.notes ?? '');
    _category = activity?.category.isNotEmpty == true
        ? activity!.category
        : _categories.first;
    if (!_categories.contains(_category)) _category = 'Other';
    _status = activity?.status.isNotEmpty == true ? activity!.status : _statuses.first;
    if (!_statuses.contains(_status)) _status = _statuses.first;
  }

  @override
  void dispose() {
    _title.dispose();
    _quantity.dispose();
    _unit.dispose();
    _amount.dispose();
    _date.dispose();
    _notes.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final isNew = widget.existing == null;
    final farmerTitle = switch (widget.type) {
      'Crops' => 'Crop record',
      'Livestock' => 'Livestock record',
      'Finance' => 'Finance record',
      'Tasks' => 'Farm task',
      'Inventory' => 'Inventory record',
      'Production' => 'Production record',
      'Equipment' => 'Equipment record',
      'Field Mapping' => 'Field mapping record',
      'Pest & Disease' => 'Pest & disease record',
      'Incidents' => 'Farm incident',
      _ => 'Farm record',
    };
    final quantityLabel = switch (widget.type) {
      'Crops' => 'Area / quantity',
      'Livestock' => 'Animal count / quantity',
      'Inventory' => 'Stock quantity',
      'Production' => 'Production quantity',
      'Field Mapping' => 'Point count (optional)',
      _ => 'Quantity / count',
    };
    final notesLabel = switch (widget.type) {
      'Inventory' => 'Supplier, batch, expiry, or stock notes',
      'Production' => 'Linked crop/livestock source, quality, or grade',
      'Field Mapping' => 'Enter coordinates (latitude, longitude) or boundary-point notes',
      'Pest & Disease' => 'Symptoms, affected area, treatment, and follow-up',
      'Incidents' => 'Impact, evidence reference, coordinates, and follow-up',
      'Tasks' => 'Instructions and follow-up notes',
      _ => 'Notes',
    };
    return Dialog.fullscreen(
      child: Scaffold(
        backgroundColor: _cream,
        appBar: AppBar(
          leading: IconButton(
            onPressed: () => Navigator.pop(context),
            icon: const Icon(Icons.close_rounded),
          ),
          title: Text(isNew ? 'Add $farmerTitle' : 'Edit $farmerTitle',
              style: const TextStyle(fontWeight: FontWeight.w800)),
          actions: [
            TextButton(onPressed: _save, child: const Text('Save')),
            const SizedBox(width: 8),
          ],
        ),
        body: Form(
          key: _formKey,
          child: ListView(
            padding: const EdgeInsets.fromLTRB(20, 8, 20, 32),
            children: [
              Container(
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(color: _mint, borderRadius: BorderRadius.circular(17)),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Icon(Icons.link_rounded, color: _green),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Text(
                        widget.type == 'Field Mapping'
                            ? 'Save field coordinates or boundary notes locally. No satellite imagery is downloaded or rendered in this module.'
                            : 'This entry will be saved under your selected farm profile and remains available offline.',
                        style: const TextStyle(color: _forest, fontSize: 12, height: 1.45),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 20),
              _field(_title, 'Title / crop / animal / purpose *',
                  icon: Icons.edit_note_rounded,
                  validator: (value) => value == null || value.trim().isEmpty
                      ? 'This field is required.'
                      : null),
              const SizedBox(height: 12),
              DropdownButtonFormField<String>(
                initialValue: _category,
                decoration: const InputDecoration(labelText: 'Category'),
                items: _categories.map((value) =>
                    DropdownMenuItem(value: value, child: Text(value))).toList(),
                onChanged: (value) => setState(() => _category = value ?? _categories.first),
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: _field(_quantity, quantityLabel,
                        keyboardType: const TextInputType.numberWithOptions(decimal: true)),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: _field(_unit, 'Unit (ha, kg, heads...)'),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              _field(_amount, 'Amount (PHP, if applicable)',
                  icon: Icons.payments_outlined,
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  validator: (value) {
                    if (value == null || value.trim().isEmpty) return null;
                    final parsed = double.tryParse(value.trim());
                    if (parsed == null || !parsed.isFinite || parsed < 0) {
                      return 'Enter a valid non-negative amount.';
                    }
                    return null;
                  }),
              const SizedBox(height: 12),
              _field(_date, 'Record date (YYYY-MM-DD)',
                  icon: Icons.calendar_month_outlined,
                  validator: (value) {
                    final parsed = DateTime.tryParse(value?.trim() ?? '');
                    return parsed == null ? 'Use YYYY-MM-DD.' : null;
                  }),
              const SizedBox(height: 12),
              DropdownButtonFormField<String>(
                initialValue: _status,
                decoration: const InputDecoration(labelText: 'Status'),
                items: _statuses.map((value) =>
                    DropdownMenuItem(value: value, child: Text(value))).toList(),
                onChanged: (value) => setState(() => _status = value ?? _statuses.first),
              ),
              const SizedBox(height: 12),
              _field(_notes, notesLabel, icon: Icons.sticky_note_2_outlined, maxLines: 4),
              const SizedBox(height: 20),
              FilledButton.icon(
                onPressed: _save,
                style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(52)),
                icon: Icon(isNew ? Icons.add_task_rounded : Icons.save_outlined),
                label: Text(isNew ? 'Save record' : 'Save changes'),
              ),
              const SizedBox(height: 10),
              const Text(
                'This record is saved on your device. Export a backup before changing phones.',
                textAlign: TextAlign.center,
                style: TextStyle(color: _muted, fontSize: 11, height: 1.45),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _field(
    TextEditingController controller,
    String label, {
    IconData? icon,
    TextInputType? keyboardType,
    int maxLines = 1,
    String? Function(String?)? validator,
  }) => TextFormField(
    controller: controller,
    keyboardType: keyboardType,
    maxLines: maxLines,
    validator: validator,
    decoration: InputDecoration(
      labelText: label,
      prefixIcon: icon == null ? null : Icon(icon, size: 20),
    ),
  );

  void _save() {
    if (!_formKey.currentState!.validate()) return;
    final now = DateTime.now().toIso8601String();
    Navigator.pop(
      context,
      FarmActivity(
        id: widget.existing?.id ?? 'ACT-${DateTime.now().microsecondsSinceEpoch}',
        farmRecordId: widget.farmRecordId,
        type: widget.type,
        title: _title.text.trim(),
        category: _category,
        quantity: _quantity.text.trim(),
        unit: _unit.text.trim(),
        amount: double.tryParse(_amount.text.trim()) ?? 0,
        date: _date.text.trim(),
        status: _status,
        notes: _notes.text.trim(),
        updatedAt: now,
      ),
    );
  }
}

class _ActivityTile extends StatelessWidget {
  const _ActivityTile({
    required this.activity,
    required this.onEdit,
    required this.onDelete,
  });

  final FarmActivity activity;
  final VoidCallback onEdit;
  final VoidCallback onDelete;

  @override
  Widget build(BuildContext context) {
    final icon = switch (activity.type) {
      'Crops' => Icons.grass_outlined,
      'Livestock' => Icons.pets_outlined,
      'Finance' => Icons.account_balance_wallet_outlined,
      'Tasks' => Icons.checklist_rounded,
      'Inventory' => Icons.inventory_2_outlined,
      'Production' => Icons.agriculture_rounded,
      'Equipment' => Icons.handyman_outlined,
      'Field Mapping' => Icons.map_outlined,
      'Pest & Disease' => Icons.biotech_outlined,
      'Incidents' => Icons.report_gmailerrorred_outlined,
      _ => Icons.folder_outlined,
    };
    final detail = [
      activity.category,
      if (activity.quantity.trim().isNotEmpty)
        '${activity.quantity} ${activity.unit}'.trim(),
      activity.date,
    ].where((value) => value.trim().isNotEmpty).join(' · ');
    final amount = activity.amount > 0
        ? '\u{20B1}${activity.amount.toStringAsFixed(2)}'
        : '';
    return Card(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(13, 13, 7, 13),
        child: Row(
          children: [
            Container(
              width: 43,
              height: 43,
              decoration: BoxDecoration(color: _mint, borderRadius: BorderRadius.circular(14)),
              child: Icon(icon, color: _green),
            ),
            const SizedBox(width: 11),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(activity.title, maxLines: 2, overflow: TextOverflow.ellipsis,
                      style: const TextStyle(color: _ink, fontWeight: FontWeight.w800, fontSize: 13)),
                  const SizedBox(height: 4),
                  Text(detail, maxLines: 2, overflow: TextOverflow.ellipsis,
                      style: const TextStyle(color: _muted, fontSize: 11)),
                  if (amount.isNotEmpty) ...[
                    const SizedBox(height: 4),
                    Text(amount, style: const TextStyle(color: _green, fontSize: 12, fontWeight: FontWeight.w900)),
                  ],
                ],
              ),
            ),
            const SizedBox(width: 6),
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 5),
                  decoration: BoxDecoration(
                    color: activity.status.toLowerCase().contains('complete') ||
                            activity.status.toLowerCase().contains('paid') ||
                            activity.status.toLowerCase().contains('received') ||
                            activity.status.toLowerCase().contains('harvested')
                        ? _mint : const Color(0xFFFFF0D7),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Text(activity.status.toUpperCase(),
                      style: const TextStyle(color: _green, fontSize: 8, fontWeight: FontWeight.w900)),
                ),
                Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    IconButton(
                      visualDensity: VisualDensity.compact,
                      tooltip: 'Edit',
                      onPressed: onEdit,
                      icon: const Icon(Icons.edit_outlined, size: 17),
                    ),
                    IconButton(
                      visualDensity: VisualDensity.compact,
                      tooltip: 'Delete',
                      onPressed: onDelete,
                      icon: const Icon(Icons.delete_outline_rounded, size: 17, color: Color(0xFFB33B32)),
                    ),
                  ],
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class ApplicationDialog extends StatefulWidget {
  const ApplicationDialog({super.key, this.existing});
  final FarmerApplication? existing;

  @override
  State<ApplicationDialog> createState() => _ApplicationDialogState();
}

class _ApplicationDialogState extends State<ApplicationDialog> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _name;
  late final TextEditingController _contact;
  late final TextEditingController _farm;
  late final TextEditingController _province;
  late final TextEditingController _municipality;
  late final TextEditingController _barangay;
  late final TextEditingController _area;
  late final TextEditingController _commodities;
  late final TextEditingController _id;
  String _tenure = 'Owned';

  @override
  void initState() {
    super.initState();
    final r = widget.existing;
    _id = TextEditingController(text: r?.id ?? '');
    _name = TextEditingController(text: r?.farmerName ?? '');
    _contact = TextEditingController(text: r?.contact ?? '');
    _farm = TextEditingController(text: r?.farmName ?? '');
    _province = TextEditingController(text: r?.province ?? '');
    _municipality = TextEditingController(text: r?.municipality ?? '');
    _barangay = TextEditingController(text: r?.barangay ?? '');
    _area = TextEditingController(text: r?.area ?? '');
    _commodities = TextEditingController(text: r?.commodities ?? '');
    _tenure = r?.landTenure.isNotEmpty == true ? r!.landTenure : 'Owned';
  }

  @override
  void dispose() {
    for (final controller in [
      _id, _name, _contact, _farm, _province, _municipality,
      _barangay, _area, _commodities,
    ]) {
      controller.dispose();
    }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final isNew = widget.existing == null;
    return Dialog.fullscreen(
      child: Scaffold(
        backgroundColor: _cream,
        appBar: AppBar(
          leading: IconButton(
            onPressed: () => Navigator.pop(context),
            icon: const Icon(Icons.close_rounded),
          ),
          title: Text(isNew ? 'Set up my farm' : 'Edit farm profile',
              style: const TextStyle(fontWeight: FontWeight.w800)),
          actions: [
            TextButton(
              onPressed: _save,
              child: const Text('Save', style: TextStyle(fontWeight: FontWeight.w800)),
            ),
            const SizedBox(width: 8),
          ],
        ),
        body: Form(
          key: _formKey,
          child: ListView(
            padding: const EdgeInsets.fromLTRB(20, 8, 20, 32),
            children: [
              Container(
                padding: const EdgeInsets.all(15),
                decoration: BoxDecoration(color: _mint, borderRadius: BorderRadius.circular(18)),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Icon(Icons.assignment_ind_outlined, color: _green, size: 24),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Text(
                        isNew
                            ? 'Add your details and farm information. Your profile is saved on this device.'
                            : 'Update your farmer details and farm information.',
                        style: const TextStyle(color: _forest, height: 1.4, fontSize: 13),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 20),
              const _FormSectionTitle(title: 'Your information', number: '01'),
              const SizedBox(height: 12),
              _field(_name, 'Your full name *', icon: Icons.person_outline,
                  validator: (v) => (v == null || v.trim().isEmpty) ? 'Your name is required' : null),
              const SizedBox(height: 10),
              _field(_id, 'Profile ID (optional)', icon: Icons.badge_outlined,
                  readOnly: !isNew),
              const SizedBox(height: 10),
              _field(_contact, 'Contact number', icon: Icons.phone_outlined,
                  keyboardType: TextInputType.phone),
              const SizedBox(height: 22),
              const _FormSectionTitle(title: 'Farm details', number: '02'),
              const SizedBox(height: 12),
              _field(_farm, 'Farm name', icon: Icons.agriculture_outlined),
              const SizedBox(height: 10),
              _field(_province, 'Province'),
              const SizedBox(height: 10),
              _field(_municipality, 'Municipality / city'),
              const SizedBox(height: 10),
              _field(_barangay, 'Barangay'),
              const SizedBox(height: 10),
              _field(_area, 'Farm area (e.g. 2.5 ha)', icon: Icons.crop_square_rounded),
              const SizedBox(height: 10),
              DropdownButtonFormField<String>(
                initialValue: _tenure,
                decoration: const InputDecoration(labelText: 'Land tenure'),
                items: const ['Owned', 'Leased', 'Agrarian reform', 'Shared / other']
                    .map((v) => DropdownMenuItem(value: v, child: Text(v))).toList(),
                onChanged: (v) => setState(() => _tenure = v ?? 'Owned'),
              ),
              const SizedBox(height: 10),
              _field(_commodities, 'Commodities / crops / livestock',
                  icon: Icons.grass_outlined, maxLines: 2),
              const SizedBox(height: 22),              const SizedBox(height: 22),
              FilledButton.icon(
                onPressed: _save,
                style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(52)),
                icon: Icon(isNew ? Icons.check_rounded : Icons.save_outlined),
                label: Text(isNew ? 'Save farm profile' : 'Save changes'),
              ),
              const SizedBox(height: 10),
              Text(
                'Saved on this device only. Export a backup before changing phones.',
                textAlign: TextAlign.center,
                style: TextStyle(color: _muted.withValues(alpha: .95), fontSize: 11, height: 1.4),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _field(
    TextEditingController controller,
    String label, {
    IconData? icon,
    TextInputType? keyboardType,
    int maxLines = 1,
    String? Function(String?)? validator,
    bool readOnly = false,
  }) =>
      TextFormField(
        controller: controller,
        keyboardType: keyboardType,
        maxLines: maxLines,
        validator: validator,
        readOnly: readOnly,
        decoration: InputDecoration(
          labelText: label,
          prefixIcon: icon == null ? null : Icon(icon, size: 20),
        ),
      );

  void _save() {
    if (!_formKey.currentState!.validate()) return;
    final now = DateTime.now().toIso8601String();
    final id = widget.existing?.id ??
        (_id.text.trim().isNotEmpty
            ? _id.text.trim()
            : 'FAR-${DateTime.now().millisecondsSinceEpoch.toString().substring(5)}');
    Navigator.pop(
      context,
      FarmerApplication(
        id: id,
        farmerName: _name.text.trim(),
        contact: _contact.text.trim(),
        farmName: _farm.text.trim(),
        province: _province.text.trim(),
        municipality: _municipality.text.trim(),
        barangay: _barangay.text.trim(),
        area: _area.text.trim(),
        landTenure: _tenure,
        commodities: _commodities.text.trim(),
        status: widget.existing?.status ?? 'Active',
        reviewNotes: widget.existing?.reviewNotes ?? '',
        updatedAt: now,
      ),
    );
  }
}

class _SectionHeading extends StatelessWidget {
  const _SectionHeading({required this.title, required this.subtitle});
  final String title;
  final String subtitle;

  @override
  Widget build(BuildContext context) => Column(
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      Text(title, style: const TextStyle(fontSize: 21, fontWeight: FontWeight.w900,
          color: _ink, letterSpacing: -.45)),
      const SizedBox(height: 4),
      Text(subtitle, style: const TextStyle(color: _muted, fontSize: 12.5, height: 1.4)),
    ],
  );
}

class _MetricCard extends StatelessWidget {
  const _MetricCard({
    required this.icon, required this.label, required this.value,
    required this.tint, required this.iconColor,
  });
  final IconData icon;
  final String label;
  final String value;
  final Color tint;
  final Color iconColor;

  @override
  Widget build(BuildContext context) => Card(
    child: Padding(
      padding: const EdgeInsets.all(13),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 34, height: 34,
            decoration: BoxDecoration(color: tint, borderRadius: BorderRadius.circular(11)),
            child: Icon(icon, color: iconColor, size: 19),
          ),
          const SizedBox(height: 13),
          Text(value, style: const TextStyle(fontSize: 24, fontWeight: FontWeight.w900, color: _ink)),
          const SizedBox(height: 3),
          Text(label, style: const TextStyle(fontSize: 10.5, color: _muted, fontWeight: FontWeight.w600)),
        ],
      ),
    ),
  );
}

class _FarmerTile extends StatelessWidget {
  const _FarmerTile({required this.record, required this.onTap});
  final FarmerApplication record;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final initials = record.farmerName.trim().split(RegExp(r'\\s+'))
        .where((part) => part.isNotEmpty).take(2)
        .map((part) => part[0].toUpperCase()).join();
    return Card(
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(22),
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Row(
            children: [
              Container(
                width: 46, height: 46,
                decoration: BoxDecoration(color: _mint, borderRadius: BorderRadius.circular(15)),
                child: Center(child: Text(initials.isEmpty ? 'F' : initials,
                    style: const TextStyle(color: _green, fontWeight: FontWeight.w900, fontSize: 15))),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(record.farmName.isEmpty ? 'My farm' : record.farmName,
                        maxLines: 1, overflow: TextOverflow.ellipsis,
                        style: const TextStyle(fontWeight: FontWeight.w800, color: _ink, fontSize: 14)),
                    const SizedBox(height: 3),
                    Text(record.farmerName,
                        maxLines: 1, overflow: TextOverflow.ellipsis,
                        style: const TextStyle(color: _green, fontSize: 12, fontWeight: FontWeight.w600)),
                    const SizedBox(height: 3),
                    Text(record.location.isEmpty ? 'Location not added' : record.location,
                        maxLines: 1, overflow: TextOverflow.ellipsis,
                        style: const TextStyle(color: _muted, fontSize: 11)),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              const Icon(Icons.edit_outlined, size: 19, color: _muted),
            ],
          ),
        ),
      ),
    );
  }
}

class _EmptyCard extends StatelessWidget {
  const _EmptyCard({
    required this.icon, required this.title, required this.message, this.action,
  });
  final IconData icon;
  final String title;
  final String message;
  final Widget? action;

  @override
  Widget build(BuildContext context) => Center(
    child: Padding(
      padding: const EdgeInsets.all(20),
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                width: 58, height: 58,
                decoration: BoxDecoration(color: _mint, borderRadius: BorderRadius.circular(18)),
                child: Icon(icon, color: _green, size: 29),
              ),
              const SizedBox(height: 14),
              Text(title, textAlign: TextAlign.center,
                  style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w900, color: _ink)),
              const SizedBox(height: 7),
              Text(message, textAlign: TextAlign.center,
                  style: const TextStyle(color: _muted, fontSize: 12, height: 1.5)),
              if (action != null) ...[const SizedBox(height: 16), action!],
            ],
          ),
        ),
      ),
    ),
  );
}

class _ReportShortcut extends StatelessWidget {
  const _ReportShortcut({required this.onTap});
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) => Card(
    color: const Color(0xFFEAF2EB),
    child: InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(22),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            const Icon(Icons.assessment_rounded, color: _green, size: 25),
            const SizedBox(width: 12),
            const Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('My Farm Reports', style: TextStyle(color: _forest, fontWeight: FontWeight.w900)),
                  SizedBox(height: 3),
                  Text('Save or share your farm records', style: TextStyle(color: _muted, fontSize: 11)),
                ],
              ),
            ),
            const Icon(Icons.arrow_forward_rounded, color: _green),
          ],
        ),
      ),
    ),
  );
}

class _InfoTile extends StatelessWidget {
  const _InfoTile({required this.icon, required this.title, required this.subtitle});
  final IconData icon;
  final String title;
  final String subtitle;

  @override
  Widget build(BuildContext context) => Card(
    child: Padding(
      padding: const EdgeInsets.all(16),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: _green, size: 24),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: const TextStyle(fontWeight: FontWeight.w800)),
                const SizedBox(height: 5),
                Text(subtitle, style: const TextStyle(color: _muted, height: 1.4, fontSize: 12)),
              ],
            ),
          ),
        ],
      ),
    ),
  );
}

class _FormSectionTitle extends StatelessWidget {
  const _FormSectionTitle({required this.title, required this.number});
  final String title;
  final String number;

  @override
  Widget build(BuildContext context) => Row(
    children: [
      Container(
        width: 32, height: 32,
        decoration: BoxDecoration(color: _forest, borderRadius: BorderRadius.circular(10)),
        child: Center(child: Text(number,
            style: const TextStyle(color: Colors.white, fontSize: 11, fontWeight: FontWeight.w900))),
      ),
      const SizedBox(width: 10),
      Text(title, style: const TextStyle(fontSize: 16, color: _ink, fontWeight: FontWeight.w900)),
    ],
  );
}
