import { getSupabaseClient, sanitizeSupabaseError } from '../lib/supabase.js';

export default async function handler(req, res) {
  if (req.method === 'OPTIONS') {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'DELETE, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
    return res.status(200).end();
  }

  if (req.method !== 'DELETE') {
    res.setHeader('Access-Control-Allow-Origin', '*');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const { id } = req.query || {};
  if (!id || typeof id !== 'string') {
    res.setHeader('Access-Control-Allow-Origin', '*');
    return res.status(400).json({ error: 'Missing or invalid survey id' });
  }

  const supabaseResult = getSupabaseClient();
  if (supabaseResult.error) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    return res.status(500).json({ error: supabaseResult.error });
  }
  const supabase = supabaseResult.client;

  const { error } = await supabase.from('surveys').delete().eq('id', id);

  if (error) {
    console.error('Supabase delete error:', error);
    res.setHeader('Access-Control-Allow-Origin', '*');
    return res.status(500).json({
      error: 'Failed to delete survey: ' + sanitizeSupabaseError(error),
    });
  }

  res.setHeader('Access-Control-Allow-Origin', '*');
  return res.status(200).json({ success: true });
}
