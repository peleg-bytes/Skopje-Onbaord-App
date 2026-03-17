import { getSupabaseClient, sanitizeSupabaseError } from '../lib/supabase.js';
import ExcelJS from 'exceljs';

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
    console.error('Supabase query error:', error);
    res.setHeader('Access-Control-Allow-Origin', '*');
    return res.status(500).json({ error: 'Failed to fetch surveys: ' + sanitizeSupabaseError(error) });
  }

  const workbook = new ExcelJS.Workbook();
  const worksheet = workbook.addWorksheet('Surveys', { headerRow: true });

  worksheet.columns = [
    { header: 'Date', key: 'date', width: 12 },
    { header: 'StartTime', key: 'startTime', width: 12 },
    { header: 'EndTime', key: 'endTime', width: 12 },
    { header: 'SubmitTime', key: 'submitTime', width: 12 },
    { header: 'SurveyorID', key: 'surveyorId', width: 14 },
    { header: 'StationName', key: 'stationName', width: 20 },
    { header: 'Latitude', key: 'latitude', width: 12 },
    { header: 'Longitude', key: 'longitude', width: 12 },
    { header: 'PassengerCount', key: 'passengerCount', width: 14 },
  ];

  const timeOnly = (v) => (v && v.includes(' ')) ? v.split(' ')[1] : (v || '');
  const serverTime = (iso) => iso ? new Date(iso).toISOString().split('T')[1].split('.')[0] : '';
  for (const r of rows || []) {
    const dateStr = r.created_at ? new Date(r.created_at).toISOString().split('T')[0] : '';
    worksheet.addRow({
      date: dateStr,
      startTime: timeOnly(r.start_time) || r.start_time || '',
      endTime: timeOnly(r.submit_time) || r.submit_time || '',
      submitTime: serverTime(r.created_at),
      surveyorId: r.surveyor_id,
      stationName: r.station_name,
      latitude: r.latitude ?? '',
      longitude: r.longitude ?? '',
      passengerCount: r.passenger_count,
    });
  }

  const stationSlug = (station || 'all').replace(/\s+/g, '_').substring(0, 30);
  const dateStr = (dateFrom && dateTo) ? `${dateFrom}_to_${dateTo}` : (dateFrom || dateTo || date || new Date().toISOString().split('T')[0]);
  const filename = `${dateStr}_${stationSlug}.xlsx`;

  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Content-Type', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
  res.setHeader('Content-Disposition', `attachment; filename="${filename}"`);

  const buffer = await workbook.xlsx.writeBuffer();
  return res.send(Buffer.from(buffer));
}
