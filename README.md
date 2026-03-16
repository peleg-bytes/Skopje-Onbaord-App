# Skopje Onboard Survey

A simple Android survey application for field surveyors to count passengers boarding buses at specific stations in Skopje, Macedonia. Surveyors use the app in the field; you download results from Israel via a web admin page.

## Overview

- **Android app**: Surveyors in Skopje count passengers at stations. Works offline; syncs when online.
- **Backend**: Hosted on Vercel + Supabase. Receives survey submissions and generates Excel exports.
- **Admin page**: You visit the web URL from Israel to download Excel files with survey results.

## Prerequisites

- **Android Studio** (or JDK 17+ and Android SDK for building APK). Set `ANDROID_HOME` to your SDK path, or open the project in Android Studio once to generate `local.properties`.
- **Node.js** 18+ (for backend)
- **Vercel CLI** (`npm i -g vercel`)
- **Supabase account** (free tier)

## Setup

### 1. Supabase

1. Create a project at [supabase.com](https://supabase.com).
2. In the SQL Editor, run the migration:

```sql
-- From backend/supabase/migrations/20260316000000_create_surveys_table.sql
create table if not exists surveys (
  id uuid default gen_random_uuid() primary key,
  surveyor_id text not null,
  station_name text not null,
  start_time text not null,
  submit_time text not null,
  latitude real,
  longitude real,
  passenger_count integer not null,
  created_at timestamptz default now()
);
create index if not exists idx_surveys_created_at on surveys (created_at);
create index if not exists idx_surveys_station_name on surveys (station_name);
```

3. In Project Settings → API, copy **Project URL** and **service_role** key.

### 2. Backend (Vercel)

1. Go to the `backend` folder:
   ```bash
   cd backend
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Deploy to Vercel:
   ```bash
   vercel deploy --prod
   ```

4. In the Vercel project dashboard, add environment variables:
   - `SUPABASE_URL` = your Supabase project URL
   - `SUPABASE_SERVICE_ROLE_KEY` = your Supabase service_role key

5. Redeploy if you added env vars after the first deploy.

6. Note your deployment URL (e.g. `https://skopje-onboard-survey.vercel.app`).

### 3. Android App

1. Open `android-app` in Android Studio (or build from command line).

2. Build the release APK:
   - **Windows**: Run `scripts\build_apk.bat`
   - **Or**: `cd android-app && ./gradlew assembleRelease`

3. APK output: `android-app/app/build/outputs/apk/release/app-release.apk`

4. Distribute the APK to surveyors (e.g. via email, USB, or file sharing).

5. **Configure API URL**: On first use, surveyors should open **Settings** (gear icon) and enter the backend URL (e.g. `https://your-app.vercel.app`). The default is `https://skopje-onboard-survey.vercel.app`.

## Survey Flow

1. **Start Survey**: Surveyor enters Station Name and Surveyor ID, taps "Start Survey".
2. **Counting**: Large counter with +5, +1, -1, -5 buttons. Count cannot go below 0. Each tap triggers vibration and sound.
3. **Reset**: "Reset Counter" with confirmation dialog.
4. **Submit**: "Done / Submit" with confirmation dialog. Survey is saved locally and queued for upload.
5. **Offline**: If the server is unreachable, surveys are stored locally and synced when the connection returns.
6. **Resume**: If the app was closed during counting, on reopen a dialog asks "Resume previous survey?".

## Downloading Results

1. Open the admin page: `https://your-app.vercel.app` (your Vercel URL).
2. Optionally select a **Date** and/or **Station name** to filter.
3. Click **Generate & Download Excel**.
4. The Excel file contains: Date, StartTime, SubmitTime, SurveyorID, StationName, Latitude, Longitude, PassengerCount.

## Scripts

| Script | Purpose |
|--------|---------|
| `scripts/build_apk.bat` | Builds the Android release APK |
| `scripts/deploy_backend.bat` | Deploys the backend to Vercel |
| `scripts/push_to_github.bat` | Adds, commits, and pushes to GitHub |

## Features

- **Offline-first**: Surveys saved locally; sync when online.
- **Auto-save**: Every counter change is saved immediately.
- **GPS**: Captures latitude/longitude when available.
- **Languages**: Macedonian (default) and English.
- **Theme**: Light, Dark, or System.
- **KEEP_SCREEN_ON**: Screen stays on while counting.

## Data Safety

- Surveys are stored in SQLite on the device.
- Submitted surveys are queued for upload until the server is reachable.
- WorkManager retries uploads periodically (every 15 minutes) when online.

## Constraints

- No authentication for survey submission.
- Admin page is public; consider Vercel password protection if needed.
- APK distribution only (no Google Play).
- Minimal dependencies; designed for low-end Android devices (Android 8+).
