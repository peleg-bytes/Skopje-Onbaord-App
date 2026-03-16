import { getSupabaseClient, sanitizeSupabaseError } from '../lib/supabase.js';

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

  const { data: rows, error } = await supabase
    .from('surveys')
    .select('start_time, station_name, created_at')
    .order('created_at', { ascending: false });

  if (error) {
    console.error('Export options Supabase error:', error);
    res.setHeader('Access-Control-Allow-Origin', '*');
    return res.status(500).json({ error: 'Database error: ' + sanitizeSupabaseError(error) });
  }

  const dateSet = new Set();
  const stationSet = new Set();
  const combos = new Map();

  for (const r of rows || []) {
    const dateStr = r.created_at ? new Date(r.created_at).toISOString().split('T')[0] : null;
    const station = r.station_name || 'Unknown';
    if (dateStr) dateSet.add(dateStr);
    stationSet.add(station);
    const key = `${dateStr}|${station}`;
    combos.set(key, (combos.get(key) || 0) + 1);
  }

  const exportOptions = Array.from(combos.entries()).map(([key, count]) => {
    const [date, station] = key.split('|');
    return { date, station, count };
  }).sort((a, b) => b.date.localeCompare(a.date) || a.station.localeCompare(b.station));

  res.setHeader('Access-Control-Allow-Origin', '*');
  return res.status(200).json({
    dates: Array.from(dateSet).sort().reverse(),
    stations: Array.from(stationSet).sort(),
    exportOptions,
  });
}
