import { formatSurveyCalendarDate, getSurveyLocalHour } from './display-timezone.js';

/**
 * Shared analysis aggregation logic.
 * @param {import('@supabase/supabase-js').SupabaseClient} supabase
 * @param {{ dateFrom?: string, dateTo?: string }} params
 */
export async function runAnalysis(supabase, { dateFrom, dateTo } = {}) {
  let query = supabase
    .from('surveys')
    .select('station_name, passenger_count, created_at')
    .order('created_at', { ascending: true });

  if (dateFrom) {
    query = query.gte('created_at', `${dateFrom}T00:00:00.000Z`);
  }
  if (dateTo) {
    query = query.lte('created_at', `${dateTo}T23:59:59.999Z`);
  }

  const { data: rows, error } = await query;
  if (error) throw error;

  const byStation = new Map();
  const byDate = new Map();
  const byHour = new Map();
  const timeSlots = [
    { key: '6-9', label: 'Morning (6–9)', hours: [6, 7, 8], passengers: 0, surveys: 0 },
    { key: '9-12', label: 'Late morning (9–12)', hours: [9, 10, 11], passengers: 0, surveys: 0 },
    { key: '12-14', label: 'Midday (12–14)', hours: [12, 13], passengers: 0, surveys: 0 },
    { key: '14-17', label: 'Afternoon (14–17)', hours: [14, 15, 16], passengers: 0, surveys: 0 },
    { key: '17-19', label: 'Peak evening (17–19)', hours: [17, 18], passengers: 0, surveys: 0 },
    { key: '19-22', label: 'Evening (19–22)', hours: [19, 20, 21], passengers: 0, surveys: 0 },
    { key: '22-6', label: 'Night (22–6)', hours: [22, 23, 0, 1, 2, 3, 4, 5], passengers: 0, surveys: 0 },
  ];
  const slotMap = new Map(timeSlots.map((s) => [s.key, { ...s }]));
  const morningPeakHours = [7, 8, 9];
  const eveningPeakHours = [17, 18];
  const morningPeakByStation = new Map();
  const eveningPeakByStation = new Map();
  let totalSurveys = 0;
  let totalPassengers = 0;

  for (const r of rows || []) {
    const station = r.station_name || 'Unknown';
    const count = Number(r.passenger_count) || 0;
    const dateStr = r.created_at ? formatSurveyCalendarDate(r.created_at) : '';
    const hour = getSurveyLocalHour(r.created_at);

    totalSurveys += 1;
    totalPassengers += count;

    const st = byStation.get(station) || { station, surveys: 0, passengers: 0 };
    st.surveys += 1;
    st.passengers += count;
    byStation.set(station, st);

    if (dateStr) {
      const dt = byDate.get(dateStr) || { date: dateStr, surveys: 0, passengers: 0 };
      dt.surveys += 1;
      dt.passengers += count;
      byDate.set(dateStr, dt);
    }

    if (hour !== null) {
      const h = byHour.get(hour) || { hour, surveys: 0, passengers: 0 };
      h.surveys += 1;
      h.passengers += count;
      byHour.set(hour, h);

      for (const slot of timeSlots) {
        if (slot.hours.includes(hour)) {
          const s = slotMap.get(slot.key);
          s.passengers += count;
          s.surveys += 1;
          break;
        }
      }

      if (morningPeakHours.includes(hour)) {
        const m = morningPeakByStation.get(station) || { station, surveys: 0, passengers: 0 };
        m.surveys += 1;
        m.passengers += count;
        morningPeakByStation.set(station, m);
      }
      if (eveningPeakHours.includes(hour)) {
        const e = eveningPeakByStation.get(station) || { station, surveys: 0, passengers: 0 };
        e.surveys += 1;
        e.passengers += count;
        eveningPeakByStation.set(station, e);
      }
    }
  }

  const stations = Array.from(byStation.values()).sort((a, b) => b.passengers - a.passengers);
  const dates = Array.from(byDate.values()).sort((a, b) => b.date.localeCompare(a.date));
  const hours = Array.from(byHour.entries())
    .map(([h, v]) => ({ ...v, hour: h }))
    .sort((a, b) => a.hour - b.hour);
  const byTimeSlot = Array.from(slotMap.values()).filter((s) => s.passengers > 0 || s.surveys > 0);

  const morningStations = Array.from(morningPeakByStation.values()).sort((a, b) => b.passengers - a.passengers);
  const eveningStations = Array.from(eveningPeakByStation.values()).sort((a, b) => b.passengers - a.passengers);
  const peakMorningTotal = morningStations.reduce((sum, s) => sum + s.passengers, 0);
  const peakEveningTotal = eveningStations.reduce((sum, s) => sum + s.passengers, 0);

  const peakHours = {
    morning: {
      label: '7–9',
      passengers: peakMorningTotal,
      surveys: morningStations.reduce((sum, s) => sum + s.surveys, 0),
      byStation: morningStations,
    },
    evening: {
      label: '17–19',
      passengers: peakEveningTotal,
      surveys: eveningStations.reduce((sum, s) => sum + s.surveys, 0),
      byStation: eveningStations,
    },
  };

  const avgPerSurvey = totalSurveys > 0 ? Math.round((totalPassengers / totalSurveys) * 10) / 10 : 0;

  return {
    summary: { totalSurveys, totalPassengers, avgPassengersPerSurvey: avgPerSurvey },
    byStation: stations,
    byDate: dates,
    byHour: hours,
    byTimeSlot,
    peakHours,
  };
}
