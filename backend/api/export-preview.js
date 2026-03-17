import { getSupabaseClient, sanitizeSupabaseError } from '../lib/supabase.js';

/**
 * Returns the same row data as the Excel export, for in-browser preview.
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

  const { date, dateFrom, dateTo, station } = req.query || {};

  const supabaseResult = getSupabaseClient();
  if (supabaseResult.error) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    return res.status(500).json({ error: supabaseResult.error });
  }
  const supabase = supabaseResult.client;

  let query = supabase.from('surveys').select('*').order('created_at', { ascending: true });

  if (dateFrom || dateTo || date) {
    const from = dateFrom || date;
    const to = dateTo || date;
    if (from) query = query.gte('created_at', `${from}T00:00:00.000Z`);
    if (to) query = query.lte('created_at', `${to}T23:59:59.999Z`);
  }

  if (station) {
    query = query.ilike('station_name', `%${station}%`);
  }

  const { data: rows, error } = await query;

  if (error) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    return res.status(500).json({ error: 'Failed to fetch: ' + sanitizeSupabaseError(error) });
  }

  const timeOnly = (v) => (v && v.includes(' ')) ? v.split(' ')[1] : (v || '');
  const serverTime = (iso) => iso ? new Date(iso).toISOString().split('T')[1].split('.')[0] : '';
  const previewRows = (rows || []).map((r) => ({
    date: r.created_at ? new Date(r.created_at).toISOString().split('T')[0] : '',
    startTime: timeOnly(r.start_time) || r.start_time || '',
    endTime: timeOnly(r.submit_time) || r.submit_time || '',
    submitTime: serverTime(r.created_at),
    surveyorId: r.surveyor_id,
    stationName: r.station_name,
    latitude: r.latitude ?? '',
    longitude: r.longitude ?? '',
    passengerCount: r.passenger_count,
  }));

  res.setHeader('Access-Control-Allow-Origin', '*');
  return res.status(200).json({ rows: previewRows });
}
