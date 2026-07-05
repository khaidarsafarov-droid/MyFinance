# Настройка Supabase для Logistics Tracker

## 1. Переменные окружения

Скопируйте `.env.local.example` в `.env.local` и заполните:

```
NEXT_PUBLIC_SUPABASE_URL=https://ваш-проект.supabase.co
NEXT_PUBLIC_SUPABASE_ANON_KEY=ваш_anon_key
```

Ключи берутся из панели Supabase: **Project Settings → API**.

---

## 2. SQL: Создание таблиц и политик

Выполните в **SQL Editor** Supabase скрипт из `supabase/migrations/001_initial_schema.sql`:

1. Откройте Supabase Dashboard → **SQL Editor**
2. Создайте новый запрос
3. Вставьте содержимое `supabase/migrations/001_initial_schema.sql`
4. Нажмите **Run**

Скрипт создаёт:
- таблицу `profiles` (связь с `auth.users`)
- таблицы `loads`, `companies`, `company_changes` с `user_id`
- триггер автособатия профиля при регистрации
- Row Level Security (RLS) для доступа только к своим данным

---

## 3. Google OAuth (по желанию)

Для входа через Google:

1. **Authentication → Providers → Google** → включить
2. Указать **Client ID** и **Client Secret** из [Google Cloud Console](https://console.cloud.google.com/apis/credentials)
3. В Google Cloud: **Authorized redirect URIs** добавить:
   ```
   https://ваш-проект.supabase.co/auth/v1/callback
   ```
4. Для локальной разработки добавить:
   ```
   http://localhost:3000/auth/callback
   ```

---

## 4. Подтверждение email

По умолчанию Supabase требует подтверждение email при регистрации. Если нужно отключить:
- **Authentication → Providers → Email** → **Confirm email** выключить

---

## 5. Дополнительные миграции

После `001_initial_schema.sql` выполните по порядку:
- `002_crowdsourcing_analytics.sql`, `003_heatmap.sql`, `004_equipment_type.sql`
- `005_rpm_thresholds.sql` — добавляет `rpm_min_threshold` и `rpm_target_threshold` в `profiles` (пороги прибыльности RPM)

---

## 6. Итоговый чеклист

- [ ] `.env.local` заполнен `NEXT_PUBLIC_SUPABASE_URL` и `NEXT_PUBLIC_SUPABASE_ANON_KEY`
- [ ] Выполнен SQL-скрипт `001_initial_schema.sql`
- [ ] RLS включён для `profiles`, `loads`, `companies`, `company_changes`
- [ ] Триггер `handle_new_user` создан
- [ ] (Опционально) Google OAuth настроен
