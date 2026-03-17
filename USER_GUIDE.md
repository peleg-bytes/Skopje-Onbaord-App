# Skopje Onboard Survey — User Guide

## What is this app?

Skopje Onboard Survey helps you count passengers getting on buses at different stations in Skopje. Surveyors use the app on their phone while they’re at the station. Later, you can open a website to download the results and see charts and summaries.

The app works without internet. If you’re offline, it saves everything on your phone and sends it to the server when you’re back online. You can use it in Macedonian or English, and choose light, dark, or system theme.

---

## For surveyors: using the app

### Getting started

Open the app and enter the station name (for example, the bus stop or station you’re at) and your surveyor ID. Then tap **Start Survey**.

### Counting passengers

You’ll see a big counter with buttons to add or subtract passengers: **+5**, **+1**, **-1**, **-5**. Tap them as people board. The count can’t go below zero. The screen stays on so you don’t have to keep unlocking it, and every change is saved automatically.

### If you make a mistake

Tap **Reset Counter** to set the count back to zero. You’ll be asked to confirm.

### When you’re done

Tap **Done / Submit** and confirm. If you’re online, you’ll see “Submitted successfully.” If you’re offline, you’ll see “Saved. Will sync when online.” — the survey will be sent later when the app has internet.

### Other options

- Tap the **gear icon** to open Settings. You can change the language, theme, or backend URL.
- If you press the back button while counting, the app will ask whether to discard the current survey or keep it.

---

## For administrators: using the website

Open your admin page in a browser (for example, `https://your-app.vercel.app`).

### Surveys page

The **Surveys** tab shows all survey records by default. You can:

- **Filter** — Use **From date**, **To date**, and **Station** to narrow the results, then **Load** to apply.
- **Preview** — See the filtered data in a popup before downloading.
- **Download Excel** — Download the filtered surveys as an Excel file.
- **Download All Excels** — Get a ZIP of all exports (by date and station).
- **Download Aggregated Excel** — One Excel file with all surveys and a column showing which export each row came from.
- **Delete** — Remove a survey from the table (one row at a time).

### Map

The **Map** tab shows stations on a map and loads automatically when you open it. Each station appears as a circle; the size reflects passenger count (larger = more passengers). Click a circle to see details. Use the filters to narrow results: **From date**, **To date**, **Station** (name), **Min passengers**, **Max passengers**. Filters apply as you type or change values. Click **Refresh** to re-fetch data when you change the date range.

### Analysis and charts

The **Analysis** tab shows charts and summaries. Pick a date range, tap **Apply date filter**, and you’ll see:

- Total passengers and surveys
- Passengers by station (bar chart)
- Passengers by time of day (pie chart)
- **Morning peak (7–9)** — passengers and how they’re spread across stations
- **Evening peak (17–19)** — same for the evening rush

You can **Export Excel** to download the analysis.

---

## What do the Excel columns mean?

| Column | What it means |
|--------|----------------|
| Date | The date of the survey |
| StartTime | When the surveyor started counting |
| EndTime | When the surveyor pressed submit |
| SubmitTime | When the data reached the server (can be later if they were offline) |
| SurveyorID | The surveyor’s ID |
| StationName | The station name |
| Latitude / Longitude | GPS coordinates (if location was allowed) |
| PassengerCount | Number of passengers counted |

---

## A few tips

**Location** — If surveyors allow location permission, the Excel will include GPS coordinates.

**Offline** — Surveys are stored on the phone and sync automatically when there’s internet. No need to do anything.

**Resume** — If someone closes the app while counting, they can resume the same survey when they open it again.
