import { getVisionModel } from "./geminiClient";
import { getWeekInfoFromDateString } from "../../utils/weekUtils";

const DIESEL_PROMPT = `
Ты — ассистент для анализа чеков за дизельное топливо дальнобойщика.
Извлеки данные из документа. Верни ТОЛЬКО валидный JSON без markdown.

{
  "documentType": "diesel",
  "date": "YYYY-MM-DD",
  "weekNumber": number,
  "year": number,
  "totalAmount": number,
  "gallons": number | null,
  "pricePerGallon": number | null,
  "location": string | null,
  "vendor": string | null,
  "currency": "USD",
  "confidence": "high" | "medium" | "low"
}

ВАЖНО:
- totalAmount — итоговая сумма оплаты за топливо
- date — дата заправки из чека
- weekNumber вычисли из даты`;

export interface ParsedDiesel {
  weekNumber: number;
  year: number;
  weekLabel: string;
  weekStartDate: Date;
  weekEndDate: Date;
  totalAmount: number;
  gallons: number | null;
  pricePerGallon: number | null;
  location: string | null;
  vendor: string | null;
  confidence: string;
}

export async function parseDieselWithGemini(
  fileBuffer: Buffer,
  mimeType: string
): Promise<ParsedDiesel | null> {
  const model = getVisionModel();
  const base64 = fileBuffer.toString("base64");

  const result = await model.generateContent([
    { text: DIESEL_PROMPT },
    {
      inlineData: {
        mimeType,
        data: base64,
      },
    },
  ]);

  const raw = result.response.text().trim();
  const cleaned = raw.replace(/```json|```/g, "").trim();
  let data: Record<string, unknown>;
  try {
    data = JSON.parse(cleaned) as Record<string, unknown>;
  } catch {
    return null;
  }

  const dateStr = (data.date as string) || new Date().toISOString().slice(0, 10);
  const weekInfo = getWeekInfoFromDateString(dateStr);

  return {
    weekNumber: (data.weekNumber as number) ?? weekInfo.weekNumber,
    year: (data.year as number) ?? weekInfo.year,
    weekLabel: weekInfo.weekLabel,
    weekStartDate: new Date(weekInfo.weekStartDate),
    weekEndDate: new Date(weekInfo.weekEndDate),
    totalAmount: Number(data.totalAmount) || 0,
    gallons: typeof data.gallons === "number" ? data.gallons : null,
    pricePerGallon: typeof data.pricePerGallon === "number" ? data.pricePerGallon : null,
    location: (data.location as string) || null,
    vendor: (data.vendor as string) || null,
    confidence: (data.confidence as string) || "low",
  };
}
