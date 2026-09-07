# Digital Wallet

A private, offline-first Android app for everything you'd otherwise keep in a paper folder,
a notes app, or a dozen different browser tabs: cards, IDs, passwords, contacts, day-to-day
finances, and client sales — all encrypted on-device, with no cloud sync and no account to
sign into.

Package: `app.orionmd.digitalwallet`
Current version: see `app/src/main/assets/changelog.txt` (also viewable in-app under
**Settings → Changelog**).

Built and shipped entirely from an Android phone — no PC, no Android Studio, no Termux. See
`SETUP_INSTRUCTIONS.md` for how the GitHub Actions build pipeline that makes that possible is
wired up.

## What's inside

**Wallet** — Cards and IDs in two sub-tabs.
- Cards: issuer, card name, cardholder name, number, expiration, CVV (hidden by default, with
  a show/hide toggle), an optional linked website/portal login, credit limit, and billing
  address — plus front/back photos.
- IDs: driver's license, state ID, Social Security card, or passport, each with front/back
  photos.
- Tap a photo to view it full-screen; a small camera badge is how you replace it.
- Drag the handle on a row to put cards or IDs in whatever order you like.

**Passwords** — a bank/social/website/investment password vault with a category per entry,
sortable Alphabetically, by Type, or Manually (drag to reorder).

**Contacts** — a fully separate, manually-entered contact list (not read from your phone's
contacts), with a category (Family/Friend/Work/Business/Other) and the same three sort modes
as Passwords.

**Finances at a Glance** — income/expense tracking, fed two ways:
- Manual entry, with an optional custom label (Deposit, Withdrawal, Debit, Credit...) instead
  of the default Income/Expense wording.
- Statement import: upload a bank/credit-card/investment statement (PDF or a photo of a paper
  one) and on-device OCR (ML Kit) finds candidate transactions for you to review and confirm —
  nothing is ever saved without a look first, and nothing leaves the device.
- A **Portfolio** section for manually-tracked stock, crypto, and cash holdings, with computed
  gain/loss, collapsible when it gets long.
- A consolidated **monthly PDF** (real aligned columns, logo watermark) rolling up income,
  expenses, portfolio, and full transaction detail for a given month.
- Optional beginning/ending balance fields on an imported statement — a plain record of what
  the statement itself says, never a number the app computes.

**Sales** — services sold to clients: date, client ID, service, price, commission and partner
fees, an always-computed net fee, and a Paid / Partial / Unpaid status with amount-paid and
balance-due. Entries are auto-numbered by creation order.

**Every list** (Cards, IDs, Passwords, Contacts, Sales) can be displayed as Tiles, Compact, or
a flat List — each screen remembers its own choice.

**Settings** — change your PIN (re-encrypts everything under the new one), export/import an
encrypted backup, see the version and changelog, and a Buy Me a Coffee link.

## Security model

- A PIN, set on first launch, derives the encryption key — there is no recovery if it's
  forgotten, by design. Nothing is stored in plaintext, including inside backups.
- Every sensitive field (card numbers, passwords, notes, contact details, financial figures,
  and so on) is individually encrypted with **AES-256-GCM** before it touches the database, and
  decrypted only in memory when displayed. See `security/CryptoManager.kt`.
- Uploaded card/ID/contact photos and statement files are stored encrypted on-device; nothing
  is ever uploaded anywhere.
- Changing your PIN re-encrypts every field and file under the new key in one pass.
- Backup export/import moves the same encrypted bytes as-is — a backup only restores correctly
  under the PIN it was made with.

## Tech stack

- Kotlin, single-Activity + Fragments, ViewBinding
- Room (SQLite) for storage, with every schema change as a hand-written `Migration` (never a
  destructive fallback) — see `data/AppDatabase.kt` for the full migration history
- ML Kit on-device OCR + PdfBox-Android for statement text extraction and PDF generation
- Material Components, a custom "navy + glass" theme
- GitHub Actions for CI: builds a release APK for `arm64-v8a` and `armeabi-v7a` on every push

## Project layout

```
app/src/main/java/app/orionmd/digitalwallet/
  data/          Room entities, DAOs, repositories (the only layer that encrypts/decrypts)
  security/      CryptoManager (AES-256-GCM) and PIN handling
  ui/wallet/     Cards + IDs
  ui/passwords/  Passwords
  ui/contacts/   Contacts
  ui/finances/   Finances at a Glance + Portfolio
  ui/sales/      Sales
  ui/statements/ Statement import, OCR parsing, monthly PDF generation
  ui/settings/   Settings, backup export/import
  ui/common/     Shared UI helpers (image picker/viewer, drag-to-reorder, sort/view modes)
```

## Building it yourself

If you have Android Studio: open the project root and build/run as normal — `minSdk 26`,
`compileSdk`/`targetSdk 34`.

If you're doing this the same way it was built (from a phone, via GitHub's mobile site and
GitHub Actions, no computer involved), see `SETUP_INSTRUCTIONS.md` for the full walkthrough —
it covers the three workflow files (`unzip.yml`, `build-apk.yml`, `bootstrap.yml`), the
one-time repo bootstrap, and how every later update gets built and versioned automatically.

## A couple of things worth knowing

- **PIN reminder**: there is no password-reset flow. If you forget your PIN, the data is
  gone — that's the trade-off for everything being encrypted with a key nobody but you has.
- **Statement OCR is a best-effort helper**, not a guaranteed parser for any particular bank's
  layout — always review the candidate transactions before confirming them.
- **Signing**: release builds are currently debug-signed, which is fine for installing on your
  own devices. Distributing this beyond that would need a real release keystore first.
