import { createClient } from '@supabase/supabase-js';

const SUPABASE_URL_HELP =
  'Use your project API URL: https://<project-ref>.supabase.co (from Supabase Dashboard → Settings → API). Do NOT use the dashboard page URL.';

/**
 * Validates SUPABASE_URL format and returns a client or error.
 * @returns {{ client: import('@supabase/supabase-js').SupabaseClient } | { error: string }}
 */
export function getSupabaseClient() {
  const supabaseUrl = process.env.SUPABASE_URL;
  const supabaseKey = process.env.SUPABASE_SERVICE_ROLE_KEY || process.env.SUPABASE_SECRET_KEY;

  if (!supabaseUrl || !supabaseKey) {
    return { error: 'Server configuration error: SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY (or SUPABASE_SECRET_KEY) must be set in Vercel.' };
  }

  const url = supabaseUrl.replace(/\/$/, '');
  if (!/^https:\/\/[a-z0-9-]+\.supabase\.co$/.test(url)) {
    return { error: 'Invalid SUPABASE_URL. ' + SUPABASE_URL_HELP };
  }

  return { client: createClient(supabaseUrl, supabaseKey) };
}

/**
 * Sanitizes Supabase error message for API response.
 * Detects HTML responses (wrong URL) and returns a helpful message.
 */
export function sanitizeSupabaseError(error) {
  const msg = (error?.message || '').trim();
  if (msg.startsWith('<!') || msg.includes('<html') || msg.includes('<!DOCTYPE')) {
    return 'Invalid SUPABASE_URL. ' + SUPABASE_URL_HELP;
  }
  return msg || 'Database error';
}
