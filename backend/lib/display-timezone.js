/**
 * Format server timestamps for survey UI/exports.
 * Device-reported start_time / submit_time are local wall-clock; created_at must use the same
 * calendar day and clock interpretation — Europe/Skopje for this project.
 */

export const SURVEY_DISPLAY_TZ = 'Europe/Skopje';

/** YYYY-MM-DD in Skopje for an ISO instant (e.g. Supabase timestamptz). */
export function formatSurveyCalendarDate(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  return new Intl.DateTimeFormat('en-CA', {
    timeZone: SURVEY_DISPLAY_TZ,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(d);
}

/** HH:mm:ss (24h) in Skopje for an ISO instant. */
export function formatSurveyClockTime(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  const parts = new Intl.DateTimeFormat('en-GB', {
    timeZone: SURVEY_DISPLAY_TZ,
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  }).formatToParts(d);
  const pick = (t) => parts.find((p) => p.type === t)?.value ?? '00';
  return `${pick('hour')}:${pick('minute')}:${pick('second')}`;
}

/** Local hour 0–23 in Skopje (for peak / time-slot analysis). */
export function getSurveyLocalHour(iso) {
  if (!iso) return null;
  const d = new Date(iso);
  const parts = new Intl.DateTimeFormat('en-GB', {
    timeZone: SURVEY_DISPLAY_TZ,
    hour: '2-digit',
    hour12: false,
  }).formatToParts(d);
  const h = parts.find((p) => p.type === 'hour')?.value;
  return h !== undefined ? parseInt(h, 10) : null;
}
