import { getSupabaseClient, sanitizeSupabaseError } from '../lib/supabase.js';

/**
 * Returns stations with location (avg lat/lng) and passenger/survey counts for map display.
 * Only includes stations that have at least one survey with coordinates.
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

  const supabaseResult = getSupabaseClient();
  if (supabaseResult.error) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    return res.status(500).json({ error: supabaseResult.error });
  }
  const supabase = supabaseResult.client;
  const { dateFrom, dateTo } = req.query || {};

  let query = supabase
    .from('surveys')
    .select('station_name, latitude, longitude, passenger_count')
    .not('latitude', 'is', null)
    .not('longitude', 'is', null);

  if (dateFrom) {
    query = query.gte('created_at', `${dateFrom}T00:00:00.000Z`);
  }
  if (dateTo) {
    query = query.lte('created_at', `${dateTo}T23:59:59.999Z`);
  }

  const { data: rows, error } = await query;

  if (error) {
    console.error('Stations map Supabase error:', error);
    res.setHeader('Access-Control-Allow-Origin', '*');
    return res.status(500).json({ error: 'Failed to fetch: ' + sanitizeSupabaseError(error) });
  }

  const byStation = new Map();
  for (const r of rows || []) {
    const station = r.station_name || 'Unknown';
    const lat = parseFloat(r.latitude);
    const lng = parseFloat(r.longitude);
    const count = Number(r.passenger_count) || 0;
    if (isNaN(lat) || isNaN(lng)) continue;

    const existing = byStation.get(station);
    if (!existing) {
      byStation.set(station, {
        station,
        latSum: lat,
        lngSum: lng,
        count: 1,
        passengers: count,
      });
    } else {
      existing.latSum += lat;
      existing.lngSum += lng;
      existing.count += 1;
      existing.passengers += count;
    }
  }

  const stations = Array.from(byStation.values()).map((s) => ({
    station: s.station,
    lat: s.latSum / s.count,
    lng: s.lngSum / s.count,
    passengers: s.passengers,
    surveys: s.count,
  }));

  res.setHeader('Access-Control-Allow-Origin', '*');
  return res.status(200).json({ stations });
}
