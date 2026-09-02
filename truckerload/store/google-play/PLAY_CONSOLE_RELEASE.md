# Google Play release runbook — TruckoRig (`com.truckorig`)

Ship checklist for version **1.5.7** (`versionCode` **12**).

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
- [ ] `versionName=1.5.7`, `versionCode=12`

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
| What’s new | `RELEASE_NOTES_1.5.7.md` (user-facing section) |

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
4. Countries / pricing: free, distribute where you support EN/RU/ES

## 5) Post-upload

- [ ] Register Play App Signing SHA-1 in Google Cloud OAuth  
- [ ] Watch Crashlytics / ANRs 24–48h (`docs/POST_RELEASE_MONITORING.md`)  
- [ ] Keep this upload keystore backed up offline

## Blockers this repo cannot finish for you

| Item | Why |
| --- | --- |
| Signing keystore + passwords | Secret; not in git |
| Public privacy-policy HTTPS URL | Needs your hosting |
| Play Console account / listing creation | Manual |
| Production Maps / Supabase / backend URLs | Your infra |

## Related docs

- `docs/GOOGLE_SIGNIN_SETUP.md`  
- `docs/RELEASE_CHECKLIST.md`  
- `store/google-play/README.md` (image specs)  
- `docs/RELEASE_PACKET_INDEX.md`
