import { getVisionModel } from "./geminiClient";
import { parsePaycheckWithGemini } from "./parsePaycheck";
import { parseDieselWithGemini } from "./parseDiesel";
import type { ParsedPaycheck } from "./parsePaycheck";
import type { ParsedDiesel } from "./parseDiesel";

const CLASSIFY_PROMPT = `
Посмотри на этот документ и определи что это.
Верни ТОЛЬКО одно слово: "paycheck" или "diesel" или "unknown"

paycheck — settlement statement, pay stub, earnings statement, выплата водителю
diesel — fuel receipt, fuel invoice, чек за топливо, дизель
unknown — если не можешь определить`;

export type FileParseResult =
  | { type: "paycheck"; data: ParsedPaycheck }
  | { type: "diesel"; data: ParsedDiesel }
  | { type: "unknown"; data: null };

export async function classifyAndParseFile(
  fileBuffer: Buffer,
  fileName: string,
  mimeType: string
): Promise<FileParseResult> {
  const model = getVisionModel();
  const base64 = fileBuffer.toString("base64");

  const classResult = await model.generateContent([
    { text: CLASSIFY_PROMPT },
    { inlineData: { mimeType, data: base64 } },
  ]);

  const docType = classResult.response.text().trim().toLowerCase().replace(/["']/g, "");

  if (docType.includes("paycheck")) {
    const data = await parsePaycheckWithGemini(fileBuffer, mimeType);
    if (data) return { type: "paycheck", data };
  }
  if (docType.includes("diesel")) {
    const data = await parseDieselWithGemini(fileBuffer, mimeType);
    if (data) return { type: "diesel", data };
  }

  return { type: "unknown", data: null };
}
