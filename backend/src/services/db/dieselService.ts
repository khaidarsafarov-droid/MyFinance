import { prisma } from "../../lib/prisma";
import type { ParsedDiesel } from "../gemini/parseDiesel";

export async function saveDiesel(
  data: ParsedDiesel,
  rawText: string,
  fileName: string | null,
  telegramMsgId?: number,
  telegramChatId?: string
) {
  await prisma.diesel.create({
    data: {
      weekNumber: data.weekNumber,
      year: data.year,
      weekLabel: data.weekLabel,
      weekStartDate: data.weekStartDate,
      weekEndDate: data.weekEndDate,
      totalAmount: data.totalAmount,
      gallons: data.gallons,
      pricePerGallon: data.pricePerGallon,
      location: data.location,
      vendor: data.vendor,
      rawText,
      fileName,
      telegramMsgId,
      telegramChatId,
    },
  });
}
