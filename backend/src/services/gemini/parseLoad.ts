import { aiGenerateText } from "./aiClient";

const LOAD_PROMPT = `
Ты — ассистент для анализа данных о грузовых перевозках Amazon Relay.
Получи текст и извлеки данные. Верни ТОЛЬКО валидный JSON без markdown.

JSON структура:
{
  "tripId": string,
  "date": "YYYY-MM-DD",
  "totalRate": number,
  "totalMiles": number,
  "pointA": string,
  "pointB": string,
  "puCount": number,
  "delCount": number,
  "stops": [
    {
      "stopNumber": number,
      "type": "PU" | "DEL",
      "puNumber": string | null,
      "note": string | null,
      "scheduledTime": string,
      "timezone": string,
      "facilityCode": string | null,
      "fullAddress": string,
      "city": string,
      "state": string,
      "zip": string
    }
  ],
  "penalties": [
    { "description": string, "amount": number }
  ]
}`;

export interface ParsedStop {
  stopNumber: number;
  type: "PU" | "DEL";
  puNumber: string | null;
  note: string | null;
  scheduledTime: string;
  timezone: string;
  facilityCode: string | null;
  fullAddress: string;
  city: string;
  state: string;
  zip: string;
}

export interface ParsedPenalty {
  description: string;
  amount: number;
}

export interface ParsedLoad {
  id: string;
  tripId: string;
  date: Date;
  totalRate: number;
  totalMiles: number;
  pointA: string;
  pointB: string;
  puCount: number;
  delCount: number;
  weekNumber: number;
  year: number;
  rawMessage: string;
  stops: ParsedStop[];
  penalties: ParsedPenalty[];
}

function getWeekNumberAndYear(date: Date): { weekNumber: number; year: number } {
  const d = new Date(date);
  d.setHours(0, 0, 0, 0);
  const oneJan = new Date(d.getFullYear(), 0, 1);
  const weekNumber = Math.ceil((((d.getTime() - oneJan.getTime()) / 86400000) + oneJan.getDay() + 1) / 7);
  return { weekNumber, year: d.getFullYear() };
}

export async function parseLoadWithGemini(text: string): Promise<ParsedLoad | null> {
  const prompt = `Текст:\n"""\n${text}\n"""`;
  const raw = await aiGenerateText(prompt, LOAD_PROMPT);
  if (!raw) return null;
  const cleaned = raw.replace(/```json|```/g, "").trim();
  let data: Record<string, unknown>;
  try {
    data = JSON.parse(cleaned) as Record<string, unknown>;
  } catch {
    return null;
  }

  const tripId = (data.tripId as string) || "T-UNKNOWN";
  const dateStr = (data.date as string) || new Date().toISOString().slice(0, 10);
  const date = new Date(dateStr);
  const { weekNumber, year } = getWeekNumberAndYear(date);
  const stops = (data.stops as ParsedStop[]) || [];
  const penalties = (data.penalties as ParsedPenalty[]) || [];

  return {
    id: tripId,
    tripId,
    date,
    totalRate: Number(data.totalRate) || 0,
    totalMiles: Number(data.totalMiles) || 0,
    pointA: (data.pointA as string) || "",
    pointB: (data.pointB as string) || "",
    puCount: Number(data.puCount) ?? stops.filter((s) => s.type === "PU").length,
    delCount: Number(data.delCount) ?? stops.filter((s) => s.type === "DEL").length,
    weekNumber,
    year,
    rawMessage: text,
    stops,
    penalties,
  };
}
