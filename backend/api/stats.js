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
    return res.status(500).json({ error: 'Server configuration error' });
  }

  const supabase = createClient(supabaseUrl, supabaseKey);

  const { count, error: countError } = await supabase.from('surveys').select('*', { count: 'exact', head: true });

  if (countError) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    return res.status(500).json({ error: 'Failed to fetch stats' });
  }

  const { data: recent } = await supabase
    .from('surveys')
    .select('station_name, created_at')
    .order('created_at', { ascending: false })
    .limit(20);

  const stations = [...new Set((recent || []).map((r) => r.station_name).filter(Boolean))];

  res.setHeader('Access-Control-Allow-Origin', '*');
  return res.status(200).json({
    totalSurveys: count ?? 0,
    recentStations: stations,
  });
}
