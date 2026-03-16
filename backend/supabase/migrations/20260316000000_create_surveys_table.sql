-- Skopje Onboard Survey - surveys table
-- Run this in Supabase SQL Editor: Dashboard > SQL Editor > New query

create table if not exists public.surveys (
  id uuid default gen_random_uuid() primary key,
  surveyor_id text not null,
  station_name text not null,
  start_time text not null,
  submit_time text not null,
  latitude real,
  longitude real,
  passenger_count integer not null,
  created_at timestamptz default now()
);

-- Indexes for common queries
create index if not exists idx_surveys_created_at on public.surveys (created_at);
create index if not exists idx_surveys_station_name on public.surveys (station_name);
