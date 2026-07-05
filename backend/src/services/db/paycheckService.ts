import { prisma } from "../../lib/prisma";
import type { ParsedPaycheck } from "../gemini/parsePaycheck";

export async function savePaycheck(
  data: ParsedPaycheck,
  rawText: string,
  fileName: string | null,
  telegramMsgId?: number,
  telegramChatId?: string
) {
  await prisma.paycheck.upsert({
    where: {
      weekNumber_year: { weekNumber: data.weekNumber, year: data.year },
    },
    create: {
      weekNumber: data.weekNumber,
      year: data.year,
      weekLabel: data.weekLabel,
      weekStartDate: data.weekStartDate,
      weekEndDate: data.weekEndDate,
      driverName: data.driverName,
      grossAmount: data.grossAmount,
      netAmount: data.netAmount,
      rawText,
      fileName,
      telegramMsgId,
      telegramChatId,
    },
    update: {
      weekLabel: data.weekLabel,
      weekStartDate: data.weekStartDate,
      weekEndDate: data.weekEndDate,
      driverName: data.driverName,
      grossAmount: data.grossAmount,
      netAmount: data.netAmount,
      rawText,
      fileName,
      telegramMsgId,
      telegramChatId,
    },
  });
}
