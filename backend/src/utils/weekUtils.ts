import { getISOWeek, getYear, startOfISOWeek, endOfISOWeek, format, parseISO } from "date-fns";

export interface WeekInfo {
  weekNumber: number;
  year: number;
  weekStartDate: string;
  weekEndDate: string;
  weekLabel: string;
}

export function getWeekInfo(date: Date): WeekInfo {
  const weekNumber = getISOWeek(date);
  const year = getYear(date);
  const weekStart = startOfISOWeek(date);
  const weekEnd = endOfISOWeek(date);
  return {
    weekNumber,
    year,
    weekStartDate: format(weekStart, "yyyy-MM-dd"),
    weekEndDate: format(weekEnd, "yyyy-MM-dd"),
    weekLabel: `${format(weekStart, "MMM d")} – ${format(weekEnd, "MMM d, yyyy")}`,
  };
}

export function getWeekInfoFromDateString(dateStr: string): WeekInfo {
  const date = parseISO(dateStr);
  return getWeekInfo(date);
}
