# Website polish QA — 2026-10-25

The first-use link opens configuration steps. A desktop QR opens the official homepage; the footer links back to the portfolio. Published APKs and checksum bytes are unchanged.

Validated in a local browser at 1440 × 1000 and 390 × 844; the page also has no horizontal overflow at 320 px. Visible images loaded. Social metadata resolves to a 1200 × 630 image. Screenshots below are unmodified browser captures of the candidate.

Checks passed: JavaScript syntax and Wrangler dry run; both published APKs and SHA256SUMS.txt matched repository SHA-256 values. The displayed QR was independently decoded with Apple Vision.

This is website acceptance. Live installation, authentication, production hardware/network operations, and native client acceptance are outside this change. A successful local check does not prove production deployment; the reviewed commit must be deployed and read back separately.

![Desktop candidate](website-polish-preview.png)

![Mobile candidate](website-polish-mobile.png)
