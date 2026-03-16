import { getSupabaseClient, sanitizeSupabaseError } from '../lib/supabase.js';

/**
 * Returns aggregated analysis: by station, by date, totals, peak hours.
 */
export default async function handler(req, res) {
  if (req.method === 'OPTIONS') {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
    return res.status(200).end();
  }

  if (req.method !== 'GET') {
    res.setHeader('Access-Control-Allow-Origin', '*');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const { dateFrom, dateTo } = req.query || {};

  const supabaseResult = getSupabaseClient();
  if (supabaseResult.error) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    return res.status(500).json({ error: supabaseResult.error });
  }
  const supabase = supabaseResult.client;

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

  if (error) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    return res.status(500).json({ error: 'Failed to fetch: ' + sanitizeSupabaseError(error) });
  }

  const byStation = new Map();
  const byDate = new Map();
  const byHour = new Map();
  let totalSurveys = 0;
  let totalPassengers = 0;

  for (const r of rows || []) {
    const station = r.station_name || 'Unknown';
    const count = Number(r.passenger_count) || 0;
    const created = r.created_at ? new Date(r.created_at) : null;
    const dateStr = created ? created.toISOString().split('T')[0] : '';
    const hour = created ? created.getUTCHours() : null;

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
    }
  }

  const stations = Array.from(byStation.values()).sort((a, b) => b.passengers - a.passengers);
  const dates = Array.from(byDate.values()).sort((a, b) => b.date.localeCompare(a.date));
  const hours = Array.from(byHour.entries())
    .map(([h, v]) => ({ ...v, hour: h }))
    .sort((a, b) => a.hour - b.hour);

  const avgPerSurvey = totalSurveys > 0 ? Math.round((totalPassengers / totalSurveys) * 10) / 10 : 0;

  res.setHeader('Access-Control-Allow-Origin', '*');
  return res.status(200).json({
    summary: {
      totalSurveys,
      totalPassengers,
      avgPassengersPerSurvey: avgPerSurvey,
    },
    byStation: stations,
    byDate: dates,
    byHour: hours,
  });
}
