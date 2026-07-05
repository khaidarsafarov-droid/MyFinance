# Google Maps API Key Security

## Checklist

1. **Restrict API key** (Google Cloud Console → APIs & Services → Credentials)
   - Edit your key → Application restrictions
   - Choose "HTTP referrers" and add:
     - `http://localhost:3000/*` (development)
     - `https://yourdomain.com/*` (production)
   - Save

2. **Restrict APIs**
   - Under "API restrictions"
   - Select "Restrict key"
   - Enable only: **Maps JavaScript API**
   - Disable all others (Geocoding, Places, etc. unless needed)

3. **Never commit `.env.local`**
   - Already in `.gitignore`
   - Use `.env.example` as a template for new setups

4. **Production env vars**
   - Add `NEXT_PUBLIC_GOOGLE_MAPS_API_KEY` in your host (Vercel, etc.) as an environment variable
   - Do not hardcode keys in source

## Key leakage

If your key is exposed:
1. Regenerate it in Cloud Console
2. Update `.env.local` and production env vars
3. Review usage/billing in Cloud Console
