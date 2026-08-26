#!/usr/bin/env python3
"""Генератор SQL с демо-данными для скриншотов Google Play.

Собирает 12 отчётных недель правдоподобной истории в стиле Amazon Relay, чтобы
журнал, цель недели, графики аналитики и финансовые экраны были не пустыми.

ВНИМАНИЕ: скрипт начинается с DELETE по таблицам loads/stops/diesel/paychecks/
misc_expenses — он предназначен только для одноразового демо-аккаунта на
эмуляторе, не для реальной базы водителя.

Даты жёстко привязаны к неделе съёмки: при пересъёмке поменяйте WEEK_START,
LAST_WEEK_START и TODAY, иначе фильтр «Эта неделя» окажется пустым.

Запуск: python3 scripts/seed_play_store_demo_data.py > demo.sql
Подробности: docs/play-store/README.md
"""
import datetime as dt
import random

TZ_OFFSET_H = -5  # plausible US-central offset for epoch millis
random.seed(20260826)

# Chain of lanes the "driver" runs, looping across the country.
LANES = [
    ("SWF2", "Garner", "NC", "TOL5", "Perrysburg", "OH", 612),
    ("TOL5", "Perrysburg", "OH", "MDW2", "Joliet", "IL", 268),
    ("MDW2", "Joliet", "IL", "STL8", "St. Louis", "MO", 295),
    ("STL8", "St. Louis", "MO", "DFW7", "Dallas", "TX", 632),
    ("DFW7", "Dallas", "TX", "PHX5", "Phoenix", "AZ", 1065),
    ("PHX5", "Phoenix", "AZ", "ONT8", "Ontario", "CA", 358),
    ("ONT8", "Ontario", "CA", "SLC2", "Salt Lake City", "UT", 690),
    ("SLC2", "Salt Lake City", "UT", "DEN4", "Denver", "CO", 525),
    ("DEN4", "Denver", "CO", "OMA2", "Omaha", "NE", 540),
    ("OMA2", "Omaha", "NE", "MSP1", "Shakopee", "MN", 380),
    ("MSP1", "Shakopee", "MN", "MKE1", "Kenosha", "WI", 335),
    ("MKE1", "Kenosha", "WI", "CVG3", "Hebron", "KY", 385),
    ("CVG3", "Hebron", "KY", "ATL6", "Atlanta", "GA", 405),
    ("ATL6", "Atlanta", "GA", "SWF2", "Garner", "NC", 375),
]

ZIPS = {
    "Garner": "27529", "Perrysburg": "43551", "Joliet": "60436", "St. Louis": "63147",
    "Dallas": "75241", "Phoenix": "85043", "Ontario": "91761", "Salt Lake City": "84116",
    "Denver": "80022", "Omaha": "68138", "Shakopee": "55379", "Kenosha": "53144",
    "Hebron": "41048", "Atlanta": "30349",
}

TRIP_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ0123456789"
WEEK_START = dt.date(2026, 6, 7)   # Sunday, reporting week 24
LAST_WEEK_START = dt.date(2026, 8, 23)  # Sunday, reporting week 35
TODAY = dt.date(2026, 8, 26)


def ms(day, hh, mm=0):
    d = dt.datetime.combine(day, dt.time(hh, mm)) - dt.timedelta(hours=TZ_OFFSET_H)
    return int(d.replace(tzinfo=dt.timezone.utc).timestamp() * 1000)


def q(s):
    return "'" + str(s).replace("'", "''") + "'"


def week_number(day):
    """Calendar.WEEK_OF_YEAR with firstDayOfWeek=SUNDAY, minimalDaysInFirstWeek=1."""
    jan1 = dt.date(day.year, 1, 1)
    first_week_start = jan1 - dt.timedelta(days=(jan1.weekday() + 1) % 7)
    return (day - first_week_start).days // 7 + 1


def trip_id():
    return "T-" + "".join(random.choice(TRIP_ALPHABET) for _ in range(9))


out = ["PRAGMA foreign_keys=ON;", "BEGIN;",
       "DELETE FROM stops;", "DELETE FROM loads;",
       "DELETE FROM diesel;", "DELETE FROM paychecks;", "DELETE FROM misc_expenses;"]

lane_idx = 0
load_no = 0
week_start = WEEK_START
week_gross = {}

while week_start <= LAST_WEEK_START:
    is_current = week_start == LAST_WEEK_START
    # Mon/Tue/Thu/Fri style cadence; the running week stops at "today".
    day_offsets = [1, 2, 4, 5] if not is_current else [0, 1, 2, 3]
    gross_week = 0.0
    miles_week = 0.0
    for offset in day_offsets:
        day = week_start + dt.timedelta(days=offset)
        if day > TODAY:
            break
        puc, pucity, pust, delc, delcity, delst, base_miles = LANES[lane_idx % len(LANES)]
        lane_idx += 1
        load_no += 1
        miles = base_miles + random.randint(-25, 25)
        rpm = round(random.uniform(2.18, 2.92), 2)
        rate = round(miles * rpm, 0)
        days = 2.0 if miles > 900 else (1.5 if miles > 600 else 1.0)
        load_id = "demo-%03d" % load_no
        trip = trip_id()
        date = day.isoformat()
        del_date = (day + dt.timedelta(days=int(days))).isoformat()
        pu_addr = "%s, %s, %s" % (puc, pucity, pust)
        del_addr = "%s, %s, %s" % (delc, delcity, delst)
        pu_cs = "%s, %s" % (pucity, pust)
        del_cs = "%s, %s" % (delcity, delst)
        raw = ("Trip ID: %s\nTotal Rate: %.2f\nTotal Loaded Miles: %d mi\n"
               "Pu-address: %s\nDel-address: %s" % (trip, rate, miles, pu_addr, del_addr))
        gross_week += rate
        miles_week += miles
        out.append(
            "INSERT INTO loads (id,tripId,date,totalRate,totalMiles,pointA,pointB,puCount,delCount,"
            "weekNumber,year,rawMessage,parsedAt,updatedAt,firstPuMillis,lastDelMillis,route,"
            "firstPuCityState,lastDelCityState,durationDays,pace,stopCount,isDispute,disputeCompleted,"
            "disputeApplyToLoad,disputeAmountApplied,actualFinishDate,equipmentType) VALUES ("
            "{id},{trip},{date},{rate},{miles},{pa},{pb},1,1,{wk},{yr},{raw},{pms},{pms},{pms},{dms},"
            "{route},{pcs},{dcs},{days},{pace},2,0,0,0,0,{fin},'DRY_VAN');".format(
                id=q(load_id), trip=q(trip), date=q(date), rate=float(rate), miles=float(miles),
                pa=q(pu_addr), pb=q(del_addr), wk=week_number(day), yr=day.year, raw=q(raw),
                pms=ms(day, 7, 30), dms=ms(dt.date.fromisoformat(del_date), 14),
                route=q("%s → %s" % (pu_cs, del_cs)), pcs=q(pu_cs), dcs=q(del_cs),
                days=days, pace=round(miles / days, 1), fin=q(del_date)))
        out.append(
            "INSERT INTO stops (loadId,stopNumber,type,puNumber,scheduledTime,timezone,facilityCode,"
            "fullAddress,city,state,zip) VALUES ({id},1,'PU',{pn},{t},'America/Chicago',{fc},{addr},"
            "{city},{st},{zip});".format(
                id=q(load_id), pn=q(trip.replace("T-", "")), t=q(date + " 07:30"), fc=q(puc),
                addr=q(pu_addr), city=q(pucity), st=q(pust), zip=q(ZIPS.get(pucity, "00000"))))
        out.append(
            "INSERT INTO stops (loadId,stopNumber,type,scheduledTime,timezone,facilityCode,"
            "fullAddress,city,state,zip) VALUES ({id},2,'DEL',{t},'America/Chicago',{fc},{addr},"
            "{city},{st},{zip});".format(
                id=q(load_id), t=q(del_date + " 14:00"), fc=q(delc), addr=q(del_addr),
                city=q(delcity), st=q(delst), zip=q(ZIPS.get(delcity, "00000"))))

    week_gross[week_start] = (gross_week, miles_week)
    week_start += dt.timedelta(days=7)

RU_MONTHS = {6: "июн", 7: "июл", 8: "авг"}
FUEL_STOPS = ["Love's #442, Council Bluffs, IA", "Pilot #328, Barstow, CA",
              "TA #77, Effingham, IL", "Flying J #612, Amarillo, TX",
              "Love's #219, Cordele, GA", "Pilot #501, Laramie, WY"]

for i, (start, (gross, miles)) in enumerate(sorted(week_gross.items())):
    if gross <= 0:
        continue
    end = start + dt.timedelta(days=6)
    label = "%s %d – %s %d, %d" % (RU_MONTHS.get(start.month, ""), start.day,
                                   RU_MONTHS.get(end.month, ""), end.day, end.year)
    ppg = round(random.uniform(3.74, 3.99), 3)
    gallons = round(miles / random.uniform(6.6, 7.4), 1)
    out.append(
        "INSERT INTO diesel (weekNumber,year,weekLabel,weekStartDate,weekEndDate,totalAmount,"
        "gallons,pricePerGallon,discountPricePerGallon,location,rawExtractedText,addedAt) VALUES "
        "({wk},{yr},{lab},{s},{e},{amt},{gal},{ppg},{dppg},{loc},'',{ts});".format(
            wk=week_number(start), yr=start.year, lab=q(label), s=q(start.isoformat()),
            e=q(end.isoformat()), amt=round(gallons * ppg, 2), gal=gallons, ppg=ppg,
            dppg=round(ppg - 0.42, 3), loc=q(FUEL_STOPS[i % len(FUEL_STOPS)]), ts=ms(end, 18)))
    out.append(
        "INSERT INTO paychecks (weekNumber,year,weekLabel,weekStartDate,weekEndDate,driverName,"
        "grossAmount,netAmount,rawExtractedText,addedAt) VALUES "
        "({wk},{yr},{lab},{s},{e},'Alex',{g},{n},'',{ts});".format(
            wk=week_number(start), yr=start.year, lab=q(label), s=q(start.isoformat()),
            e=q(end.isoformat()), g=round(gross, 2), n=round(gross * 0.754, 2), ts=ms(end, 20)))

MISC = [("2026-08-24", 68.40, "Парковка, Joliet IL"),
        ("2026-08-21", 145.00, "Мойка трейлера"),
        ("2026-08-18", 32.90, "Перчатки и стяжки"),
        ("2026-08-12", 220.00, "Замена ламп и щёток"),
        ("2026-07-29", 96.00, "Весы CAT, Amarillo TX"),
        ("2026-07-14", 310.00, "Ремонт крепления двери")]
for date, amount, desc in MISC:
    day = dt.date.fromisoformat(date)
    out.append(
        "INSERT INTO misc_expenses (amount,description,date,createdAt,updatedAt) VALUES "
        "({a},{d},{dt},{ts},{ts});".format(a=amount, d=q(desc), dt=q(date), ts=ms(day, 12)))

out.append("UPDATE driver_profile SET displayName='Алексей', truckType='Freightliner Cascadia', "
           "homeState='TX', homeHubCity='Dallas, TX', experienceYears=7, licenseClass='CDL-A', "
           "currentRoute='St. Louis, MO → Dallas, TX', status='ON_ROUTE';")
out.append("COMMIT;")

print("\n".join(out))
