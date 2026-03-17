import { getSupabaseClient, sanitizeSupabaseError } from '../lib/supabase.js';

export const config = {
  api: { bodyParser: true },
};

export default async function handler(req, res) {
  if (req.method === 'OPTIONS') {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
    return res.status(200).end();
  }

  if (req.method !== 'POST') {
    res.setHeader('Access-Control-Allow-Origin', '*');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const { surveyorId, stationName, startTime, submitTime, latitude, longitude, passengerCount } = req.body || {};

  if (!surveyorId || !stationName || !startTime || !submitTime || passengerCount === undefined) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    return res.status(400).json({ error: 'Missing required fields: surveyorId, stationName, startTime, submitTime, passengerCount' });
  }

  const supabaseResult = getSupabaseClient();
  if (supabaseResult.error) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    return res.status(500).json({ error: supabaseResult.error });
  }
  const supabase = supabaseResult.client;

  // Reject duplicates: same surveyor + station + start_time + passenger_count (within 60s)
  const { data: existing } = await supabase
    .from('surveys')
    .select('id')
    .eq('surveyor_id', String(surveyorId))
    .eq('station_name', String(stationName))
    .eq('start_time', String(startTime))
    .eq('passenger_count', parseInt(passengerCount, 10))
    .gte('created_at', new Date(Date.now() - 60000).toISOString())
    .limit(1)
    .maybeSingle();

  if (existing) {
    console.log('[submit-survey] Duplicate skipped:', { surveyorId, stationName, startTime, passengerCount });
    res.setHeader('Access-Control-Allow-Origin', '*');
    return res.status(200).json({ success: true }); // Idempotent: treat duplicate as success
  }

  const { error } = await supabase.from('surveys').insert({
    surveyor_id: String(surveyorId),
    station_name: String(stationName),
    start_time: String(startTime),
    submit_time: String(submitTime),
    latitude: latitude != null ? parseFloat(latitude) : null,
    longitude: longitude != null ? parseFloat(longitude) : null,
    passenger_count: parseInt(passengerCount, 10),
  });

  if (error) {
    console.error('[submit-survey] Supabase insert error:', error);
    res.setHeader('Access-Control-Allow-Origin', '*');
    return res.status(500).json({
      error: 'Failed to save survey: ' + sanitizeSupabaseError(error),
    });
  }

  console.log('[submit-survey] Inserted:', { surveyorId, stationName, startTime, passengerCount });
  res.setHeader('Access-Control-Allow-Origin', '*');
  return res.status(200).json({ success: true });
}
