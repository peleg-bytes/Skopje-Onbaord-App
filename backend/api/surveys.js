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
  const supabaseKey = process.env.SUPABASE_SERVICE_ROLE_KEY;

  if (!supabaseUrl || !supabaseKey) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    return res.status(500).json({ error: 'Server configuration error' });
  }

  const supabase = createClient(supabaseUrl, supabaseKey);
  const { date, station, limit = '100' } = req.query || {};

  let query = supabase
    .from('surveys')
    .select('*')
    .order('created_at', { ascending: false })
    .limit(Math.min(parseInt(limit, 10) || 100, 500));

  if (date) {
    const startOfDay = `${date}T00:00:00.000Z`;
    const endOfDay = `${date}T23:59:59.999Z`;
    query = query.gte('created_at', startOfDay).lte('created_at', endOfDay);
  }

  if (station) {
    query = query.ilike('station_name', `%${station}%`);
  }

  const { data: surveys, error } = await query;

  if (error) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    return res.status(500).json({ error: 'Failed to fetch surveys' });
  }

  res.setHeader('Access-Control-Allow-Origin', '*');
  return res.status(200).json({ surveys: surveys || [] });
}
