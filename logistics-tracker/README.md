# Logistics Tracker

Single Page Application for tracking financial metrics of freight transportation. Built with Next.js 14, Tailwind CSS, Zustand, Recharts, and Google Maps.

## Features

- **Onboarding**: Setup company on first run (Name, Start Date)
- **Dashboard**: List of load cards sorted by date, FAB to add new loads, company change dividers
- **Load cards**: Edit state (US) for map reports; tap state badge or "+ Add state"
- **Add Load**: Form with date, gross, profit, diesel, optional state
- **Analytics**: Summary cards (Gross, Profit, Diesel) + multi-line chart + **US state map** (Google Maps)
- **Map**: Click a state → drawer with gross, profit, diesel, trips, avg rate
- **Settings**: Switch companies, add companies, load sample data for map testing
- **Persistence**: All data stored in localStorage

## Tech Stack

- Next.js 14+ (App Router)
- TypeScript
- Tailwind CSS
- Lucide React (icons)
- Recharts (charts)
- @react-google-maps/api
- Zustand (state + persist)

## Getting Started

1. Install Node.js 18+ if not already installed.

2. Copy env template and add your Google Maps API key:
   ```bash
   cp .env.example .env.local
   # Edit .env.local, set NEXT_PUBLIC_GOOGLE_MAPS_API_KEY
   ```
   Get a key at [Google Cloud Console](https://console.cloud.google.com/google/maps-apis). See [docs/API_KEY_SECURITY.md](docs/API_KEY_SECURITY.md) for security checklist.

3. Install dependencies:
   ```bash
   cd logistics-tracker
   npm install
   ```

4. Run the development server:
   ```bash
   npm run dev
   ```

4. Open [http://localhost:3000](http://localhost:3000).

## Project Structure

```
logistics-tracker/
├── src/
│   ├── app/           # App Router pages
│   ├── components/    # UI components
│   ├── hooks/        # Custom hooks (hydration)
│   ├── lib/          # Utils (feed builder, formatters)
│   ├── store/        # Zustand store
│   └── types/       # TypeScript interfaces
├── requirements.md   # Full PRD
└── package.json
```

## Git setup

```bash
git init
# .gitignore already excludes .env*.local
git add .
git commit -m "Initial commit"
git remote add origin <your-repo-url>
git push -u origin main
```

## Deployment (Vercel)

1. Push to GitHub and import the project in Vercel.
2. Add environment variable: `NEXT_PUBLIC_GOOGLE_MAPS_API_KEY`
3. Deploy. The app is static-friendly; localStorage persists per domain.

## Data Models

- **Load**: id, date, gross, profit, diesel, companyId, state? (US abbreviation)
- **Company**: id, name, isCurrent
- **CompanyChange**: id, date, companyId, companyName (for feed dividers)
