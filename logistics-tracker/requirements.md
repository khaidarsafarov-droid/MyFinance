# Logistics Tracker - Technical Specification (PRD)

## 1. Core Concept
Single Page Application (SPA) for tracking financial metrics of freight transportation. The system supports linking records to specific companies and visualizing income/expense dynamics.

## 2. Technical Stack
- Framework: Next.js 14+ (App Router)
- Styling: Tailwind CSS
- Icons: Lucide-React
- Charts: Recharts
- State Management: Zustand + localStorage persistence

## 3. Data Schema
- **Load**: id, date (ISO), gross, profit, diesel, companyId
- **Company**: id, name, isCurrent

## 4. Application Flow
- **Phase 1**: Onboarding - Setup Company (Name, Start Date) if no company in localStorage
- **Phase 2**: Dashboard - Load cards list, FAB under last card, company change dividers
- **Phase 3**: Add Load - Form with date, gross, profit, diesel
- **Phase 4**: Analytics - Summary cards + Multi-line chart

## 5. Requirements
- Persistence: localStorage
- Responsive: Mobile First
- Style: Clean Tech (rounded-2xl, shadow-sm, contrast fonts)
