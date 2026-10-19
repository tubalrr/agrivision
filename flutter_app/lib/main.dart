import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:cross_file/cross_file.dart';
import 'package:share_plus/share_plus.dart';
import 'package:shared_preferences/shared_preferences.dart';

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

String applicationsToCsv(List<FarmerApplication> records) {
  String cell(String value) {
    // Prevent spreadsheet formula execution for untrusted user-entered text.
    final dangerous = RegExp(r'^[\\s\\uFEFF]*[=+\\-@＝＋－＠]').hasMatch(value);
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
      'Application Status',
      'Review Notes',
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
          r.status,
          r.reviewNotes,
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


class AgriVisionApp extends StatelessWidget {
  const AgriVisionApp({super.key});

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
      home: const AgriHome(),
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
  String _reportType = 'Farmer Applications';
  String _reportFormat = 'CSV';
  String _reportScope = 'All farmers';
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
            ? 'Farmer application saved on this device.'
            : 'Farmer record updated.'),
        behavior: SnackBarBehavior.floating,
      ),
    );
  }

  static const _reportTypes = [
    'Farmer Applications', 'Crop Records', 'Livestock Records',
    'Finance Records', 'Task Register', 'Complete Farm Activity',
  ];

  List<Map<String, dynamic>> _reportRows() {
    final chosenRecords = _reportScope == 'Selected farmer'
        ? _records.where((r) => r.id == (_selectedFarmRecord?.id ?? '')).toList()
        : _records;
    final chosenIds = chosenRecords.map((r) => r.id).toSet();
    final activities = _activities.where((a) => chosenIds.contains(a.farmRecordId)).toList();
    final selectedType = switch (_reportType) {
      'Crop Records' => 'Crops',
      'Livestock Records' => 'Livestock',
      'Finance Records' => 'Finance',
      'Task Register' => 'Tasks',
      _ => '',
    };
    final chosenActivities = selectedType.isEmpty
        ? activities
        : activities.where((a) => a.type == selectedType).toList();

    if (_reportType == 'Farmer Applications') {
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
        'Application Status': r.status,
        'Review Notes': r.reviewNotes,
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
    if (_reportType == 'Farmer Applications') {
      final text = applicationsToCsv(_records.where((r) =>
          _reportScope == 'All farmers' || r.id == (_selectedFarmRecord?.id ?? '')).toList());
      return text;
    }
    const columns = [
      'Farm Record ID', 'Farmer Name', 'Record ID', 'Record Type', 'Title',
      'Category', 'Quantity', 'Unit', 'Amount', 'Date', 'Status', 'Notes', 'Last Updated',
    ];
    String cell(String value) {
      // Escape spreadsheet formula prefixes, including those after leading whitespace.
      final dangerous = RegExp(r'^[\\s\\uFEFF]*[=+\\-@＝＋－＠]').hasMatch(value);
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
        const SnackBar(content: Text('Walang records para sa napiling DA report.')),
      );
      return;
    }
    final now = DateTime.now();
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
    final extension = _reportFormat.toLowerCase();
    final safeType = _reportType.toLowerCase().replaceAll(RegExp(r'[^a-z0-9]+'), '-');
    final fileName = 'agrivision-$safeType-${now.toIso8601String().substring(0, 10)}.$extension';
    final mimeType = _reportFormat == 'JSON' ? 'application/json' : 'text/csv';
    await Share.shareXFiles(
      [
        XFile.fromData(
          utf8.encode(content),
          mimeType: mimeType,
        ),
      ],
      subject: 'AgriVision DA $_reportType $_reportFormat',
      fileNameOverrides: [fileName],
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
          NavigationDestination(icon: Icon(Icons.grid_view_rounded), label: 'Overview'),
          NavigationDestination(icon: Icon(Icons.groups_2_outlined), label: 'Registry'),
          NavigationDestination(icon: Icon(Icons.agriculture_outlined), label: 'Farm Data'),
          NavigationDestination(icon: Icon(Icons.assessment_outlined), label: 'DA Reports'),
          NavigationDestination(icon: Icon(Icons.tune_rounded), label: 'More'),
        ],
      ),
    );
  }

  Widget _dashboard() {
    final review = _records.where((r) => _isReview(r.status)).length;
    final approved = _records.where((r) => _isApproved(r.status)).length;
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
                      child: Icon(Icons.eco_rounded,
                          size: 130, color: Colors.white.withValues(alpha: .08)),
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
                          child: const Text('DA STAFF WORKSPACE',
                              style: TextStyle(color: Colors.white, fontSize: 10,
                                  fontWeight: FontWeight.w800, letterSpacing: 1.1)),
                        ),
                        const SizedBox(height: 18),
                        const Text('Good work starts\nwith clear records.',
                            style: TextStyle(color: Colors.white, fontSize: 27,
                                height: 1.12, fontWeight: FontWeight.w800)),
                        const SizedBox(height: 10),
                        Text(
                          'Manage farmer applications and prepare registry reports in one place.',
                          style: TextStyle(color: Colors.white.withValues(alpha: .82),
                              height: 1.45, fontSize: 13),
                        ),
                        const SizedBox(height: 20),
                        FilledButton.icon(
                          onPressed: () => _openApplication(),
                          style: FilledButton.styleFrom(
                            backgroundColor: Colors.white,
                            foregroundColor: _forest,
                            padding: const EdgeInsets.symmetric(horizontal: 15, vertical: 13),
                          ),
                          icon: const Icon(Icons.person_add_alt_1_rounded, size: 18),
                          label: const Text('Register a farmer'),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 24),
              const _SectionHeading(title: 'Registry at a glance', subtitle: 'Based on records saved on this device'),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(child: _MetricCard(
                    icon: Icons.groups_2_outlined, label: 'Total farmers',
                    value: _records.length.toString(), tint: _mint, iconColor: _green,
                  )),
                  const SizedBox(width: 10),
                  Expanded(child: _MetricCard(
                    icon: Icons.pending_actions_rounded, label: 'For review',
                    value: review.toString(), tint: const Color(0xFFFFF0D7), iconColor: _amber,
                  )),
                  const SizedBox(width: 10),
                  Expanded(child: _MetricCard(
                    icon: Icons.verified_outlined, label: 'Approved',
                    value: approved.toString(), tint: const Color(0xFFE6ECFF), iconColor: const Color(0xFF4C62A8),
                  )),
                ],
              ),
              const SizedBox(height: 24),
              Row(
                children: [
                  const Expanded(child: _SectionHeading(title: 'Recent applications', subtitle: 'Latest saved farmer records')),
                  TextButton(
                    onPressed: () => setState(() => _tab = 1),
                    child: const Text('View all'),
                  ),
                ],
              ),
              const SizedBox(height: 10),
              if (_records.isEmpty)
                const _EmptyCard(
                  icon: Icons.assignment_ind_outlined,
                  title: 'Your registry starts here',
                  message: 'Wala pang farmer record. Tap Register a farmer para gumawa ng application.',
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
                title: 'Farmer & Farm Registry',
                subtitle: 'Applications are stored locally on this device.',
              ),
              const SizedBox(height: 14),
              TextField(
                onChanged: (value) => setState(() => _search = value),
                decoration: const InputDecoration(
                  hintText: 'Search name, ID, farm or location',
                  prefixIcon: Icon(Icons.search_rounded),
                ),
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Text('${_filteredRecords.length} records',
                      style: const TextStyle(color: _muted, fontWeight: FontWeight.w600)),
                  const Spacer(),
                  FilledButton.icon(
                    onPressed: () => _openApplication(),
                    icon: const Icon(Icons.add_rounded, size: 18),
                    label: const Text('Register'),
                  ),
                ],
              ),
            ],
          ),
        ),
        Expanded(
          child: _filteredRecords.isEmpty
              ? _EmptyCard(
                  icon: _search.isEmpty ? Icons.person_add_alt_1_rounded : Icons.search_off_rounded,
                  title: _search.isEmpty ? 'No farmer applications yet' : 'No matching records',
                  message: _search.isEmpty
                      ? 'Pindutin ang Register para simulan ang farmer application.'
                      : 'Subukan ang ibang pangalan, ID, farm o lokasyon.',
                  action: _search.isEmpty
                      ? FilledButton.icon(
                          onPressed: () => _openApplication(),
                          icon: const Icon(Icons.add_rounded),
                          label: const Text('Register farmer'),
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
                title: 'Farm Operations',
                subtitle: 'Crops, livestock, finance, and tasks saved against a specific farmer record.',
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
                  title: 'Register a farmer first',
                  message: 'Farm operations must be linked to a specific farmer/farm. Register a record before adding crops, livestock, finance, or tasks.',
                  action: FilledButton.icon(
                    onPressed: () => _openApplication(),
                    icon: const Icon(Icons.person_add_alt_1_rounded),
                    label: const Text('Register farmer'),
                  ),
                )
              : visibleActivities.isEmpty
                  ? _EmptyCard(
                      icon: _activityType == 'Crops'
                          ? Icons.grass_outlined
                          : _activityType == 'Livestock'
                              ? Icons.pets_outlined
                              : _activityType == 'Finance'
                                  ? Icons.account_balance_wallet_outlined
                                  : Icons.checklist_rounded,
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

  Widget _activityChip(String type, IconData icon) => FilterChip(
    selected: _activityType == type,
    onSelected: (_) => setState(() => _activityType = type),
    avatar: Icon(icon, size: 17, color: _activityType == type ? _green : _muted),
    label: Text(type),
    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(13)),
  );

  Widget _reports() {
    final review = _records.where((r) => _isReview(r.status)).length;
    final approved = _records.where((r) => _isApproved(r.status)).length;
    final reportCount = _reportRows().length;
    final selectedName = _selectedFarmRecord?.farmerName ?? 'No farmer selected';
    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 18, 20, 28),
      children: [
        _topBar(),
        const SizedBox(height: 22),
        const _SectionHeading(
          title: 'DA Reports',
          subtitle: 'Choose a report type and scope, then share a CSV or JSON export built from locally saved data.',
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
                'DA Report Builder',
                style: TextStyle(color: Colors.white, fontSize: 21, fontWeight: FontWeight.w900),
              ),
              const SizedBox(height: 6),
              Text(
                'Prepare a focused register for ${_reportScope == 'Selected farmer' ? selectedName : 'all registered farmers'}.',
                style: TextStyle(color: Colors.white.withValues(alpha: .8), height: 1.45),
              ),
              const SizedBox(height: 18),
              const Text('Report type', style: TextStyle(color: Colors.white, fontWeight: FontWeight.w700)),
              const SizedBox(height: 7),
              _darkSelector<String>(
                value: _reportType,
                values: _reportTypes,
                label: (value) => value,
                onChanged: (value) => setState(() => _reportType = value),
              ),
              const SizedBox(height: 14),
              const Text('Report scope', style: TextStyle(color: Colors.white, fontWeight: FontWeight.w700)),
              const SizedBox(height: 7),
              _darkSelector<String>(
                value: _reportScope,
                values: const ['All farmers', 'Selected farmer'],
                label: (value) => value,
                onChanged: (value) => setState(() => _reportScope = value),
              ),
              const SizedBox(height: 14),
              const Text('Export format', style: TextStyle(color: Colors.white, fontWeight: FontWeight.w700)),
              const SizedBox(height: 7),
              Row(
                children: [
                  Expanded(child: _formatChoice('CSV')),
                  const SizedBox(width: 9),
                  Expanded(child: _formatChoice('JSON')),
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
              icon: Icons.groups_2_outlined, label: 'Farmers registered',
              value: _records.length.toString(), tint: _mint, iconColor: _green,
            )),
            const SizedBox(width: 10),
            Expanded(child: _MetricCard(
              icon: Icons.pending_actions_rounded, label: 'For review',
              value: review.toString(), tint: const Color(0xFFFFF0D7), iconColor: _amber,
            )),
            const SizedBox(width: 10),
            Expanded(child: _MetricCard(
              icon: Icons.verified_outlined, label: 'Approved',
              value: approved.toString(), tint: const Color(0xFFE6ECFF), iconColor: const Color(0xFF4C62A8),
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
                const Text('Report contents', style: TextStyle(fontWeight: FontWeight.w800, fontSize: 16)),
                const SizedBox(height: 5),
                Text(
                  _reportType == 'Farmer Applications'
                      ? 'Farmer and farm registration fields'
                      : 'Farm-linked ${_reportType.toLowerCase()} entries',
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
          'Data boundary: exports contain only records saved in this Flutter app on this device. They are not an official DA submission and are not synced to a central server.',
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
    if (_reportType == 'Farmer Applications') {
      return const [
        'Farmer/farm record IDs, farmer name, and contact number',
        'Farm name, location, area, land tenure, and commodities',
        'Application status, DA review notes, and update timestamp',
      ];
    }
    return const [
      'Farmer name and linked farm record ID',
      'Record type, title, category, date, and status',
      'Quantity, unit, amount, and notes where applicable',
    ];
  }

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
                  child: const Icon(Icons.eco_rounded, color: _green, size: 30),
                ),
                const SizedBox(width: 14),
                const Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('AgriVision', style: TextStyle(fontSize: 20, fontWeight: FontWeight.w800)),
                      SizedBox(height: 3),
                      Text('DA Staff Workspace · Flutter Edition', style: TextStyle(color: _muted, fontSize: 12)),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 12),
        const _InfoTile(
          icon: Icons.offline_pin_outlined,
          title: 'Local-first registry',
          subtitle: 'Farmer applications are saved on this device using local preferences.',
        ),
        const SizedBox(height: 10),
        const _InfoTile(
          icon: Icons.cloud_off_outlined,
          title: 'Cloud sync not connected',
          subtitle: 'Records are not shared with another staff device or central DA server yet.',
        ),
        const SizedBox(height: 10),
        const _InfoTile(
          icon: Icons.verified_user_outlined,
          title: 'Staff access',
          subtitle: 'This screen is a workspace prototype; verified DA login and permissions still need setup.',
        ),
        const SizedBox(height: 20),
        OutlinedButton.icon(
          onPressed: () => setState(() => _tab = 0),
          icon: const Icon(Icons.home_outlined),
          label: const Text('Back to overview'),
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
          child: const Icon(Icons.eco_rounded, color: Colors.white, size: 25),
        ),
        const SizedBox(width: 11),
        const Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('AgriVision', style: TextStyle(fontSize: 20, fontWeight: FontWeight.w900, letterSpacing: -.4)),
              Text('DA FIELD OPERATIONS', style: TextStyle(fontSize: 9, color: _muted,
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
    'Crops' => const ['Planting', 'Crop growth', 'Harvest', 'Pest / disease', 'Other'],
    'Livestock' => const ['Poultry', 'Cattle', 'Swine', 'Goat / sheep', 'Other'],
    'Finance' => const ['Expense', 'Income / sale', 'Assistance', 'Other'],
    _ => const ['Field inspection', 'Follow-up', 'Document review', 'Distribution', 'Other'],
  };

  List<String> get _statuses => switch (widget.type) {
    'Crops' => const ['Planned', 'In progress', 'Harvested', 'Needs review', 'Completed'],
    'Livestock' => const ['Active', 'For monitoring', 'Needs attention', 'Completed'],
    'Finance' => const ['Recorded', 'Pending', 'Paid', 'Received', 'For review'],
    _ => const ['Pending', 'In progress', 'Completed', 'Cancelled'],
  };

  @override
  void initState() {
    super.initState();
    final activity = widget.existing;
    _title = TextEditingController(text: activity?.title ?? '');
    _quantity = TextEditingController(text: activity?.quantity ?? '');
    _unit = TextEditingController(text: activity?.unit ?? (widget.type == 'Crops' ? 'ha' : ''));
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
    final farmerTitle = widget.type == 'Crops'
        ? 'Crop record'
        : widget.type == 'Livestock'
            ? 'Livestock record'
            : widget.type == 'Finance'
                ? 'Finance record'
                : 'Farm task';
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
                        'This record will be linked to farm record ${widget.farmRecordId}. Records for other farmers stay separate.',
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
                    child: _field(_quantity, widget.type == 'Crops'
                        ? 'Area / quantity' : 'Quantity / count',
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
              _field(_notes, 'Notes', icon: Icons.sticky_note_2_outlined, maxLines: 3),
              const SizedBox(height: 20),
              FilledButton.icon(
                onPressed: _save,
                style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(52)),
                icon: Icon(isNew ? Icons.add_task_rounded : Icons.save_outlined),
                label: Text(isNew ? 'Save record' : 'Save changes'),
              ),
              const SizedBox(height: 10),
              const Text(
                'Local record only. It is not submitted to a DA database automatically.',
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
      _ => Icons.checklist_rounded,
    };
    final detail = [
      activity.category,
      if (activity.quantity.trim().isNotEmpty)
        '${activity.quantity} ${activity.unit}'.trim(),
      activity.date,
    ].where((value) => value.trim().isNotEmpty).join(' · ');
    final amount = activity.amount > 0
        ? '₱${activity.amount.toStringAsFixed(2)}'
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
  late final TextEditingController _notes;
  late final TextEditingController _id;
  String _tenure = 'Owned';
  String _status = 'For Review';

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
    _notes = TextEditingController(text: r?.reviewNotes ?? '');
    _tenure = r?.landTenure.isNotEmpty == true ? r!.landTenure : 'Owned';
    _status = r?.status ?? 'For Review';
  }

  @override
  void dispose() {
    for (final controller in [
      _id, _name, _contact, _farm, _province, _municipality,
      _barangay, _area, _commodities, _notes,
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
          title: Text(isNew ? 'Farmer application' : 'Edit farmer record',
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
                            ? 'Encode the farmer and farm details. A local reference ID is created when you submit.'
                            : 'Update the record details and application review status.',
                        style: const TextStyle(color: _forest, height: 1.4, fontSize: 13),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 20),
              const _FormSectionTitle(title: 'Farmer information', number: '01'),
              const SizedBox(height: 12),
              _field(_name, 'Full name of farmer *', icon: Icons.person_outline,
                  validator: (v) => (v == null || v.trim().isEmpty) ? 'Farmer name is required' : null),
              const SizedBox(height: 10),
              _field(_id, 'Farmer / reference ID (optional)', icon: Icons.badge_outlined,
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
              const SizedBox(height: 22),
              const _FormSectionTitle(title: 'DA review', number: '03'),
              const SizedBox(height: 12),
              DropdownButtonFormField<String>(
                initialValue: _status,
                decoration: const InputDecoration(labelText: 'Application status'),
                items: const ['For Review', 'Incomplete', 'Approved', 'Rejected']
                    .map((v) => DropdownMenuItem(value: v, child: Text(v))).toList(),
                onChanged: (v) => setState(() => _status = v ?? 'For Review'),
              ),
              const SizedBox(height: 10),
              _field(_notes, 'Review notes', icon: Icons.sticky_note_2_outlined, maxLines: 3),
              const SizedBox(height: 22),
              FilledButton.icon(
                onPressed: _save,
                style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(52)),
                icon: Icon(isNew ? Icons.check_rounded : Icons.save_outlined),
                label: Text(isNew ? 'Submit application' : 'Save changes'),
              ),
              const SizedBox(height: 10),
              Text(
                'Storage: this device only. This does not submit the application to a central DA system.',
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
        status: _status,
        reviewNotes: _notes.text.trim(),
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
    final approved = _isApproved(record.status);
    final initials = record.farmerName.trim().split(RegExp(r'\s+'))
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
                    Text(record.farmerName, maxLines: 1, overflow: TextOverflow.ellipsis,
                        style: const TextStyle(fontWeight: FontWeight.w800, color: _ink, fontSize: 14)),
                    const SizedBox(height: 3),
                    Text(record.farmName.isEmpty ? 'Farm not specified' : record.farmName,
                        maxLines: 1, overflow: TextOverflow.ellipsis,
                        style: const TextStyle(color: _green, fontSize: 12, fontWeight: FontWeight.w600)),
                    const SizedBox(height: 3),
                    Text(record.location.isEmpty ? 'Location not provided' : record.location,
                        maxLines: 1, overflow: TextOverflow.ellipsis,
                        style: const TextStyle(color: _muted, fontSize: 11)),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 5),
                    decoration: BoxDecoration(
                      color: approved ? _mint : const Color(0xFFFFF0D7),
                      borderRadius: BorderRadius.circular(9),
                    ),
                    child: Text(record.status.toUpperCase(),
                        style: TextStyle(fontSize: 8, letterSpacing: .3, fontWeight: FontWeight.w900,
                            color: approved ? _green : _amber)),
                  ),
                  const SizedBox(height: 8),
                  const Icon(Icons.chevron_right_rounded, size: 19, color: _muted),
                ],
              ),
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
                  Text('DA Reports', style: TextStyle(color: _forest, fontWeight: FontWeight.w900)),
                  SizedBox(height: 3),
                  Text('Export the saved farmer register', style: TextStyle(color: _muted, fontSize: 11)),
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

bool _isApproved(String status) =>
    status.toLowerCase().contains('approved') ||
    status.toLowerCase().contains('registered') ||
    status.toLowerCase().contains('complete');

bool _isReview(String status) =>
    status.isEmpty ||
    status.toLowerCase().contains('review') ||
    status.toLowerCase().contains('pending') ||
    status.toLowerCase().contains('incomplete');
