# Google Play release runbook — TruckoRig (`com.truckorig`)

Ship checklist for version **1.5.11** (`versionCode` **16**, `targetSdk` **36**).

## 0) Prerequisites (you must do once)

1. **Upload keystore** — create or reuse (never lose this file):

   ```bash
   cd truckerload
   # follow keystore.properties.example
   cp keystore.properties.example keystore.properties
   # edit passwords; place .keystore under signing/
   ```

2. **Google Cloud / Play**
   - Android OAuth client: package **`com.truckorig`** + upload-key SHA-1  
   - After first Play upload: add **App signing certificate** SHA-1 as a second Android client  
   - Restrict Maps API key (if used) to `com.truckorig` + signing certs

3. **Privacy policy URL** — host `store/google-play/PRIVACY_POLICY.md` (or equivalent HTML) on HTTPS and paste the URL into Play Console.

## 1) Build the release bundle

```bash
cd truckerload
# Production-oriented local.properties (gitignored):
#   LOCAL_ONLY_MODE=false
#   GOOGLE_MAPS_API_KEY=...   # optional but needed for maps
# Never put TELEGRAM_BOT_TOKEN in release (forced empty).

sh ./gradlew :app:bundleRelease
# Output: app/build/outputs/bundle/release/app-release.aab
```

Optional APK smoke: `sh ./gradlew :app:assembleRelease`

Confirm:

- [ ] `verifyReleaseSecretsEmpty` passed (bot token empty)  
- [ ] AAB is **signed** (keystore.properties present)  
- [ ] `versionName=1.5.11`, `versionCode=16`

## 2) Play Console — create / update listing

**Package name:** `com.truckorig`

| Field | Source file |
| --- | --- |
| App name | `store/google-play/listing/*/title.txt` |
| Short description | `…/short-description.txt` |
| Full description | `…/full-description.txt` |
| High-res icon | `store/google-play/high-res-icon.png` |
| Feature graphic | `feature-graphic-en.png` / `feature-graphic-ru.png` |
| Phone screenshots | `phone-en/` · `phone-ru/` (or SoftUI set in `docs/play-store-screenshots-en/`) |
| Tablet | `tablet-10-en/` · `tablet-10-ru/` |
| What’s new | `RELEASE_NOTES_1.5.11.md` (user-facing section) |

## 3) App content / policy forms

- [ ] Privacy policy URL  
- [ ] Data safety — follow `DATA_SAFETY.md`  
- [ ] Photos / videos policy (camera)  
- [ ] Location declaration  
- [ ] Target audience / content ratings questionnaire  
- [ ] News apps / COVID / financial features — answer honestly (journal ≠ banking)

## 4) Release track

1. Upload `app-release.aab` to **Internal testing** first  
2. Smoke on a clean device (`docs/SMOKE_TEST_SCRIPT.md`)  
3. Promote to **Closed** → **Open** → **Production** when green  
4. Countries / pricing: the **app download stays free**. Paid access is the in-app subscription below, not a paid app price.

## 4b) Subscription — first month free, then $2.99

The app cannot set this price. Play Console is the only place the charge exists. Create it **before** you expect the Subscribe button to open a payment sheet. Billing also needs the app uploaded to at least internal testing, with the same package `com.truckorig`.

Play Console → **Monetize** → **Products** → **Subscriptions** → **Create subscription**

| Field | Value |
| --- | --- |
| Product ID | `premium_monthly_subscription` (cannot be changed later) |
| Name | TruckoRig Premium |
| Description | First month free, then monthly Premium: Telegram, analytics, weekly goal, map, taxes, scanner, camera, export, Drive backup, deadhead. |

**Base plan** (Add base plan):

| Field | Value |
| --- | --- |
| Base plan ID | `monthly` |
| Type | Auto-renewing |
| Billing period | Monthly (1 month) |
| Price | **USD 2.99**. Use Play’s price conversion for other countries, or set them yourself. |
| Activation | Activate the base plan |

**Offer** on that base plan (Add offer):

| Field | Value |
| --- | --- |
| Offer ID | `free-month` |
| Phase | Free trial, duration **1 month** |
| Tag | `free-month` |
| Eligibility | New subscribers (people who have not already used this offer) |
| Regions | Same countries as the base plan |
| Activation | Activate the offer, then activate the subscription |

The app asks Play for this product, prefers the offer tagged `free-month`, and shows Play’s own price on the button (`$2.99` in the US). After the free month, Play renews the `monthly` base plan until the user cancels. The manifest already has `com.android.vending.BILLING`.

**Try a purchase:** Play Console → **Settings** → **License testing** → add your Gmail. Install the app **from Play** (internal testing is enough), not as a sideloaded APK. License testers are not charged; test renewals are accelerated (a month can renew in minutes).

**Manage / cancel:** Settings → Premium → “Manage subscription in Google Play”, or Play → Payments and subscriptions.

Republish the privacy policy (payments section dated 2026-10-13) at https://rentry.co/truckorig-privacy-policy so the Play URL matches the draft.

## 5) Post-upload

- [ ] Register Play App Signing SHA-1 in Google Cloud OAuth  
- [ ] Watch Crashlytics / ANRs 24–48h (`docs/POST_RELEASE_MONITORING.md`)  
- [ ] Keep this upload keystore backed up offline

## Blockers this repo cannot finish for you

| Item | Why |
| --- | --- |
| Signing keystore + passwords | Secret; not in git |
| Public privacy-policy HTTPS URL | https://rentry.co/truckorig-privacy-policy |
| Play Console account / listing creation | Manual |
| Production Maps / Supabase / backend URLs | Your infra |

## Related docs

- `docs/GOOGLE_SIGNIN_SETUP.md`  
- `docs/RELEASE_CHECKLIST.md`  
- `store/google-play/README.md` (image specs)  
- `docs/RELEASE_PACKET_INDEX.md`
