# TruckoRig release documents

Drafts for Google Play and App Store submission. Nothing in this folder has been published, merged to `main`, or uploaded to Play Console, App Store Connect, or [rentry](https://rentry.co/truckorig-privacy-policy).

**Do not merge or publish until K S reviews the TODO list and hosts the policy and terms on HTTPS.**

These drafts are based on the Android repo (`MyFinance`, `main` at `b431f9b`, versionCode 18 / versionName 1.6.0) and the iOS repo (`TruckoRig-Apple`, marketing version 1.5.7 / build 1). They are not legal advice.

## Files

| File | Purpose |
| --- | --- |
| [PRIVACY_POLICY.md](PRIVACY_POLICY.md) | Full bilingual (RU + EN) policy for Android and iOS. Ready to copy to a host. |
| [privacy/index.html](privacy/index.html) | Same policy, styled for GitHub Pages (or any static host). |
| [TERMS_OF_USE.md](TERMS_OF_USE.md) | Full bilingual terms. Fill the governing-law TODO before publishing. |
| [terms/index.html](terms/index.html) | Same terms, styled for GitHub Pages. |
| [PLAY_CONSOLE_ANSWERS.md](PLAY_CONSOLE_ANSWERS.md) | Data safety, IARC, target audience, ads, app access, listing fixes, 1.6.0 release notes. |
| [APP_STORE_ANSWERS.md](APP_STORE_ANSWERS.md) | App Privacy labels, age rating, export compliance, listing copy, review notes. |

Existing repo drafts this set supersedes (do not delete them in this PR):

- `truckerload/store/google-play/PRIVACY_POLICY.md` (2026-10-13, Android-only, has payments)
- `docs/privacy/index.html` (2026-09-03, no payments / iOS / iCloud)
- Live page https://rentry.co/truckorig-privacy-policy (2026-09-03, same gap)
- `truckerload/store/google-play/DATA_SAFETY.md`
- `truckerload/store/google-play/PLAY_CONSOLE_RELEASE.md` (still mentions 1.5.11)

## How to publish later (K S)

1. Resolve every **TODO for K S** below and in the two answers files.
2. Host `privacy/index.html` and `terms/index.html` on HTTPS (GitHub Pages is fine: enable Pages on this repo, or copy the HTML into `docs/privacy/` and `docs/terms/`).
3. Paste the public URLs into Play Console and App Store Connect, then update the live rentry page so it matches, or retire it.
4. Do not put `google-services.json` in the Play AAB if you want to keep the “no developer collection / no Crashlytics” answers.

## TODO for K S

See also the marked TODOs inside `PLAY_CONSOLE_ANSWERS.md` and `APP_STORE_ANSWERS.md`.

1. **Host the policy and terms** and replace every `TODO: public HTTPS URL` placeholder.
2. **Governing law** in the Terms — confirm the state or country (suggested default: New Jersey, USA). Do not publish Terms until this is filled in.
3. **Support email mismatch.** Policy and Play/App Store contact in these drafts use `khaidar.safarov@gmail.com`. In-app improvement feedback currently opens mail to `Truckorig@gmail.com`. Pick one official address and use it everywhere.
4. **Release AAB must not include `google-services.json`** if Data safety stays “no collection.” Crashlytics is optional and off unless that file is present.
5. **Google / Apple SDK Data safety and App Privacy review.** Maps SDK, Play Billing, Play Services Auth (Drive), ML Kit document scanner, Play Services Location, and Google Maps SDK for iOS may transmit diagnostics or usage data to Google. The recommended form answers follow the existing repo draft (“no developer collection,” user-initiated Drive / Telegram / export). Confirm against each SDK’s official disclosure before submitting. If an SDK collects data on its own, change the answers to Yes and declare those types.
6. **iCloud on the first App Store build.** The iOS code and entitlements include iCloud Drive (`iCloud.com.truckorig`) and media backup. If the first TestFlight or App Store build ships without iCloud, App Groups, or the widget, trim those sections from the published policy until they ship.
7. **Play listing title is over the 30-character limit** in `truckerload/store/google-play/listing/`. Suggested replacements are in `PLAY_CONSOLE_ANSWERS.md`. The full description still says “Sign in with Google or continue with email,” which is outdated — journal login is a local first-run name only.
8. **Play app category, IARC contact email, and trader status** (EU/UK, deadline 1 February 2027).
9. **iOS marketing version** is still 1.5.7 (build 1). Bump it before submission. Confirm App Store category and whether a marketing URL is needed.
10. **Export compliance.** `Info.plist` already sets `ITSAppUsesNonExemptEncryption` to `false`. Confirm that SQLCipher / Keychain local encryption is still treated as exempt.
11. **Subscription products** must be created in Play Console and App Store Connect (`premium_monthly_subscription`, base plan / subscription period monthly, introductory offer `free-month`, US $2.99) before the Subscribe button can charge real users.
