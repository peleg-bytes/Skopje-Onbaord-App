import { getSupabaseClient, sanitizeSupabaseError } from '../lib/supabase.js';
import { runAnalysis } from '../lib/analysis.js';
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

  try {
    const { summary, byStation, byDate, byTimeSlot } = await runAnalysis(supabaseResult.client, req.query);
    const total = summary.totalPassengers || 1;

    const workbook = new ExcelJS.Workbook();

    const wsSummary = workbook.addWorksheet('Summary', { headerRow: true });
    wsSummary.columns = [
      { header: 'Metric', key: 'metric', width: 22 },
      { header: 'Value', key: 'value', width: 12 },
    ];
    wsSummary.addRow({ metric: 'Total Passengers', value: summary.totalPassengers });
    wsSummary.addRow({ metric: 'Total Surveys', value: summary.totalSurveys });
    wsSummary.addRow({ metric: 'Avg per Survey', value: summary.avgPassengersPerSurvey });

    const wsStation = workbook.addWorksheet('By Station', { headerRow: true });
    wsStation.columns = [
      { header: 'Station', key: 'station', width: 25 },
      { header: 'Passengers', key: 'passengers', width: 12 },
      { header: 'Surveys', key: 'surveys', width: 10 },
      { header: '%', key: 'pct', width: 8 },
    ];
    for (const s of byStation || []) {
      wsStation.addRow({
        station: s.station,
        passengers: s.passengers,
        surveys: s.surveys,
        pct: Math.round((s.passengers / total) * 100) + '%',
      });
    }

    const wsTimeSlot = workbook.addWorksheet('By Time Slot', { headerRow: true });
    wsTimeSlot.columns = [
      { header: 'Time Slot', key: 'label', width: 22 },
      { header: 'Passengers', key: 'passengers', width: 12 },
      { header: 'Surveys', key: 'surveys', width: 10 },
      { header: '%', key: 'pct', width: 8 },
    ];
    for (const s of byTimeSlot || []) {
      wsTimeSlot.addRow({
        label: s.label,
        passengers: s.passengers,
        surveys: s.surveys,
        pct: Math.round((s.passengers / total) * 100) + '%',
      });
    }

    const wsDate = workbook.addWorksheet('By Date', { headerRow: true });
    wsDate.columns = [
      { header: 'Date', key: 'date', width: 12 },
      { header: 'Passengers', key: 'passengers', width: 12 },
      { header: 'Surveys', key: 'surveys', width: 10 },
      { header: '%', key: 'pct', width: 8 },
    ];
    for (const s of byDate || []) {
      wsDate.addRow({
        date: s.date,
        passengers: s.passengers,
        surveys: s.surveys,
        pct: Math.round((s.passengers / total) * 100) + '%',
      });
    }

    const { dateFrom, dateTo } = req.query || {};
    const rangeStr = (dateFrom && dateTo) ? `${dateFrom}_to_${dateTo}` : (dateFrom || dateTo) || 'all';
    const filename = `analysis_${rangeStr}.xlsx`;

    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Content-Type', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
    res.setHeader('Content-Disposition', `attachment; filename="${filename}"`);

    const buffer = await workbook.xlsx.writeBuffer();
    return res.send(Buffer.from(buffer));
  } catch (err) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    return res.status(500).json({ error: 'Failed to export: ' + sanitizeSupabaseError(err) });
  }
}
