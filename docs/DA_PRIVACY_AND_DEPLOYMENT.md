# DA Privacy & Deployment Checklist

This is a practical deployment checklist, not a legal opinion. AgriVision's current Flutter edition is an on-device development workspace; the native Kotlin edition has more modules but does not by itself provide an official DA transmission endpoint or managed agency-wide records service.

## Before real beneficiary data is entered

- [ ] Name the actual personal information controller and responsible deploying organization.
- [ ] Identify the authorized purpose and applicable legal basis for each category of farmer/beneficiary data.
- [ ] Publish a final privacy notice with the actual controller/privacy officer contact and request channel.
- [ ] Define the authorized-user roles, device controls, staff training, sharing approvals, and offboarding steps.
- [ ] Define retention and secure-disposal timelines for device records, exported reports, backups, photos/evidence, and provider-side data.
- [ ] Obtain required approvals/permissions for official records and any sharing between government agencies; use an appropriate data-sharing agreement where required.
- [ ] Assess privacy risks for names, contact numbers, exact coordinates, incident evidence, assistance records, and financial details.
- [ ] Test access, backup/restore, correction, deletion, and recovery processes with synthetic test records.
- [ ] Review third-party provider terms and privacy disclosures for maps, weather, Firebase if enabled, and app-share destinations.
- [ ] Use dedicated API keys restricted by app/signature/API and never ship unrestricted secrets.
- [ ] Decide whether encryption-at-rest or managed devices/storage is required. Current Flutter JSON backup is not encrypted by an AgriVision-specific layer.
- [ ] Prepare a data-breach response process and a way to contact affected persons/authority where legally required.
- [ ] Verify official DA integration specifications and authorization before making any claim of DA connectivity.

## Important data boundary

The Flutter edition stores farmer and farm activity records locally using SharedPreferences. It does not authenticate DA staff, sync farm records to a central server, or transmit official reports to a DA backend. Its report share/export and JSON restore functions are manual device workflows.

The native Kotlin edition uses Room for operational data and has separate optional integrations. Optional Firebase support is limited to authentication/profile metadata when configured; the repository's Firestore rules deny non-profile collections by default. Open-Meteo receives saved coordinates when the native weather feature is requested. Consult [INTEGRATIONS.md](../INTEGRATIONS.md) and [PRIVACY_POLICY.md](../PRIVACY_POLICY.md).

## Philippine privacy reference

Review the [Data Privacy Act of 2012 (Republic Act No. 10173)](https://privacy.gov.ph/Data-Privacy-Act/) and the [Implementing Rules and Regulations](https://privacy.gov.ph/implementing-rules%20regulations-data-privacy-act-2012/) issued by the National Privacy Commission. Applicability and required safeguards depend on the actual deployment and processing; obtain qualified privacy/legal review before operational use.
