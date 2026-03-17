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
  const { date, dateFrom, dateTo, station, limit = '500' } = req.query || {};

  let query = supabase
    .from('surveys')
    .select('*')
    .order('created_at', { ascending: false })
    .limit(Math.min(parseInt(limit, 10) || 500, 2000));

  if (dateFrom || dateTo || date) {
    const from = dateFrom || date;
    const to = dateTo || date;
    if (from) query = query.gte('created_at', `${from}T00:00:00.000Z`);
    if (to) query = query.lte('created_at', `${to}T23:59:59.999Z`);
  }

  if (station) {
    query = query.ilike('station_name', `%${station}%`);
  }

  const { data: surveys, error } = await query;

  if (error) {
    console.error('Supabase surveys fetch error:', error);
    res.setHeader('Access-Control-Allow-Origin', '*');
    return res.status(500).json({
      error: 'Failed to fetch surveys: ' + sanitizeSupabaseError(error),
    });
  }

  res.setHeader('Access-Control-Allow-Origin', '*');
  return res.status(200).json({ surveys: surveys || [] });
}
