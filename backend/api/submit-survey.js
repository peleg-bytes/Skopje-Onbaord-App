import { createClient } from '@supabase/supabase-js';

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

  const supabaseUrl = process.env.SUPABASE_URL;
  const supabaseKey = process.env.SUPABASE_SERVICE_ROLE_KEY || process.env.SUPABASE_SECRET_KEY;

  if (!supabaseUrl || !supabaseKey) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    return res.status(500).json({ error: 'Server configuration error' });
  }

  const supabase = createClient(supabaseUrl, supabaseKey);

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
    console.error('Supabase insert error:', error);
    res.setHeader('Access-Control-Allow-Origin', '*');
    return res.status(500).json({
      error: 'Failed to save survey',
      details: error.message,
      code: error.code,
    });
  }

  res.setHeader('Access-Control-Allow-Origin', '*');
  return res.status(200).json({ success: true });
}
