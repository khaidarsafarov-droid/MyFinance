-- Optional accuracy on live presence (compact GPS payload).
alter table public.driver_presence
  add column if not exists accuracy_m double precision;
