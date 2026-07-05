import { getVisionModel } from "./geminiClient";
import { getWeekInfoFromDateString } from "../../utils/weekUtils";

const PAYCHECK_PROMPT = `
Ты — ассистент для анализа платёжных документов дальнобойщика (settlement, paycheck).
Извлеки данные из документа. Верни ТОЛЬКО валидный JSON без markdown.

{
  "documentType": "paycheck",
  "driverName": string | null,
  "weekStartDate": "YYYY-MM-DD",
  "weekEndDate": "YYYY-MM-DD",
  "weekNumber": number,
  "year": number,
  "grossAmount": number | null,
  "netAmount": number,
  "currency": "USD",
  "confidence": "high" | "medium" | "low"
}

ВАЖНО:
- netAmount — финальная сумма выплаты драйверу
- weekStartDate и weekEndDate извлеки из документа
- weekNumber — номер недели в году (1-52)
- Если не уверен — укажи confidence: "low"`;

export interface ParsedPaycheck {
  weekNumber: number;
  year: number;
  weekLabel: string;
  weekStartDate: Date;
  weekEndDate: Date;
  driverName: string | null;
  grossAmount: number | null;
  netAmount: number;
  confidence: string;
}

export async function parsePaycheckWithGemini(
  fileBuffer: Buffer,
  mimeType: string
): Promise<ParsedPaycheck | null> {
  const model = getVisionModel();
  const base64 = fileBuffer.toString("base64");

  const result = await model.generateContent([
    { text: PAYCHECK_PROMPT },
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

  const weekStartStr = (data.weekStartDate as string) || "";
  const weekEndStr = (data.weekEndDate as string) || "";
  const weekInfo = weekStartStr
    ? getWeekInfoFromDateString(weekStartStr)
    : { weekNumber: (data.weekNumber as number) || 1, year: (data.year as number) || new Date().getFullYear(), weekLabel: "", weekStartDate: "", weekEndDate: "" };

  return {
    weekNumber: weekInfo.weekNumber,
    year: weekInfo.year,
    weekLabel: weekInfo.weekLabel || `${weekStartStr} – ${weekEndStr}`,
    weekStartDate: new Date(weekInfo.weekStartDate || weekStartStr),
    weekEndDate: new Date(weekInfo.weekEndDate || weekEndStr),
    driverName: (data.driverName as string) || null,
    grossAmount: typeof data.grossAmount === "number" ? data.grossAmount : null,
    netAmount: Number(data.netAmount) || 0,
    confidence: (data.confidence as string) || "low",
  };
}
