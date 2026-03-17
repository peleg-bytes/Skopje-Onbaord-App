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

  const supabaseResult = getSupabaseClient();
  if (supabaseResult.error) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    return res.status(500).json({ error: supabaseResult.error });
  }
  const supabase = supabaseResult.client;

  const { data: rows, error } = await supabase
    .from('surveys')
    .select('*')
    .order('created_at', { ascending: true });

  if (error) {
    console.error('Supabase query error:', error);
    res.setHeader('Access-Control-Allow-Origin', '*');
    return res.status(500).json({ error: 'Failed to fetch surveys: ' + sanitizeSupabaseError(error) });
  }

  const workbook = new ExcelJS.Workbook();
  const worksheet = workbook.addWorksheet('All Surveys', { headerRow: true });

  worksheet.columns = [
    { header: 'Excel', key: 'excel', width: 28 },
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
    const stationSlug = (r.station_name || 'all').replace(/\s+/g, '_').substring(0, 25);
    const excelName = `${dateStr}_${stationSlug}.xlsx`;
    worksheet.addRow({
      excel: excelName,
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

  const filename = `aggregated_surveys.xlsx`;

  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Content-Type', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
  res.setHeader('Content-Disposition', `attachment; filename="${filename}"`);

  const buffer = await workbook.xlsx.writeBuffer();
  return res.send(Buffer.from(buffer));
}
