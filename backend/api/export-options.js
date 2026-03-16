import { createClient } from '@supabase/supabase-js';

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

  const supabaseUrl = process.env.SUPABASE_URL;
  const supabaseKey = process.env.SUPABASE_SERVICE_ROLE_KEY || process.env.SUPABASE_SECRET_KEY;

  if (!supabaseUrl || !supabaseKey) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    return res.status(500).json({ error: 'Server configuration error: SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY (or SUPABASE_SECRET_KEY) must be set in Vercel.' });
  }

  const supabase = createClient(supabaseUrl, supabaseKey);

  const { data: rows, error } = await supabase
    .from('surveys')
    .select('start_time, station_name, created_at')
    .order('created_at', { ascending: false });

  if (error) {
    console.error('Export options Supabase error:', error);
    res.setHeader('Access-Control-Allow-Origin', '*');
    return res.status(500).json({ error: 'Database error: ' + (error.message || 'Failed to fetch export options') });
  }

  const dateSet = new Set();
  const stationSet = new Set();
  const combos = new Map();

  for (const r of rows || []) {
    const dateStr = r.start_time ? r.start_time.split(' ')[0] : (r.created_at ? new Date(r.created_at).toISOString().split('T')[0] : null);
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
