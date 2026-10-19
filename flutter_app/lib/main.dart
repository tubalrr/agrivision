import 'dart:convert';

import 'package:flutter/material.dart';
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
  String cell(String value) => '"${value.replaceAll('"', '""')}"';
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
  List<FarmerApplication> _records = [];

  @override
  void initState() {
    super.initState();
    _loadRecords();
  }

  Future<void> _loadRecords() async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString('agrivision_flutter_registry_v1');
    final records = <FarmerApplication>[];
    if (raw != null) {
      try {
        final decoded = jsonDecode(raw) as List<dynamic>;
        records.addAll(decoded.map((e) =>
            FarmerApplication.fromJson(Map<String, dynamic>.from(e as Map))));
      } catch (_) {
        // Keep the app usable if a previous local cache is malformed.
      }
    }
    if (!mounted) return;
    setState(() {
      _records = records;
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

  Future<void> _exportApplications() async {
    if (_records.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Wala pang farmer application na ie-export.')),
      );
      return;
    }
    await SharePlus.instance.share(
      ShareParams(
        text: applicationsToCsv(_records),
        subject: 'AgriVision DA Farmer Applications CSV',
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final pages = [
      _dashboard(),
      _registry(),
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
              _ReportShortcut(onTap: () => setState(() => _tab = 2)),
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

  Widget _reports() {
    final review = _records.where((r) => _isReview(r.status)).length;
    final approved = _records.where((r) => _isApproved(r.status)).length;
    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 18, 20, 28),
      children: [
        _topBar(),
        const SizedBox(height: 22),
        const _SectionHeading(
          title: 'DA Reports',
          subtitle: 'Prepare a shareable report from saved application records.',
        ),
        const SizedBox(height: 18),
        Container(
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            color: _forest,
            borderRadius: BorderRadius.circular(24),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Icon(Icons.description_outlined, color: Colors.white, size: 30),
              const SizedBox(height: 16),
              const Text('Farmer Applications Register',
                  style: TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.w800)),
              const SizedBox(height: 7),
              Text(
                'Export all ${_records.length} saved applications as a CSV file that can be opened in spreadsheet apps.',
                style: TextStyle(color: Colors.white.withValues(alpha: .78), height: 1.45),
              ),
              const SizedBox(height: 18),
              FilledButton.icon(
                onPressed: _exportApplications,
                style: FilledButton.styleFrom(
                  backgroundColor: Colors.white,
                  foregroundColor: _forest,
                  minimumSize: const Size.fromHeight(48),
                ),
                icon: const Icon(Icons.ios_share_rounded),
                label: const Text('Export / Share CSV'),
              ),
            ],
          ),
        ),
        const SizedBox(height: 18),
        Row(
          children: [
            Expanded(child: _MetricCard(
              icon: Icons.pending_actions_rounded, label: 'For review',
              value: review.toString(), tint: const Color(0xFFFFF0D7), iconColor: _amber,
            )),
            const SizedBox(width: 12),
            Expanded(child: _MetricCard(
              icon: Icons.verified_outlined, label: 'Approved / complete',
              value: approved.toString(), tint: _mint, iconColor: _green,
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
                const Text('Included fields', style: TextStyle(fontWeight: FontWeight.w800, fontSize: 16)),
                const SizedBox(height: 12),
                ...[
                  'Farmer and farm record IDs',
                  'Farmer name and contact number',
                  'Farm location, area and land tenure',
                  'Commodities and application status',
                  'DA review notes and last-updated date',
                ].map((text) => Padding(
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
          'Note: this Flutter build currently exports the Farmer Applications register. Other DA report categories and PDF/JSON exports will be added in the next migration stages.',
          style: TextStyle(color: _muted, fontSize: 12, height: 1.5),
        ),
      ],
    );
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
