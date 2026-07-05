import { prisma } from "../../lib/prisma";
import type { ParsedLoad } from "../gemini/parseLoad";

export async function createLoad(
  parsed: ParsedLoad,
  telegramMsgId?: number,
  telegramChatId?: string
) {
  const { stops, penalties, ...loadData } = parsed;
  await prisma.load.create({
    data: {
      id: parsed.id,
      tripId: parsed.tripId,
      date: parsed.date,
      totalRate: parsed.totalRate,
      totalMiles: parsed.totalMiles,
      pointA: parsed.pointA,
      pointB: parsed.pointB,
      puCount: parsed.puCount,
      delCount: parsed.delCount,
      weekNumber: parsed.weekNumber,
      year: parsed.year,
      rawMessage: parsed.rawMessage,
      telegramMsgId,
      telegramChatId,
      parsedAt: parsed.date,
      stops: {
        create: stops.map((s) => ({
          stopNumber: s.stopNumber,
          type: s.type,
          puNumber: s.puNumber,
          note: s.note,
          scheduledTime: new Date(s.scheduledTime || parsed.date),
          timezone: s.timezone,
          facilityCode: s.facilityCode,
          fullAddress: s.fullAddress,
          city: s.city,
          state: s.state,
          zip: s.zip,
        })),
      },
      penalties: {
        create: penalties.map((p) => ({ description: p.description, amount: p.amount })),
      },
    },
  });
}

export async function updateLoad(
  tripId: string,
  parsed: ParsedLoad,
  telegramMsgId?: number,
  telegramChatId?: string
) {
  await prisma.stop.deleteMany({ where: { loadId: tripId } });
  await prisma.penalty.deleteMany({ where: { loadId: tripId } });
  const { stops, penalties, ...loadData } = parsed;
  await prisma.load.update({
    where: { tripId },
    data: {
      date: parsed.date,
      totalRate: parsed.totalRate,
      totalMiles: parsed.totalMiles,
      pointA: parsed.pointA,
      pointB: parsed.pointB,
      puCount: parsed.puCount,
      delCount: parsed.delCount,
      weekNumber: parsed.weekNumber,
      year: parsed.year,
      rawMessage: parsed.rawMessage,
      telegramMsgId,
      telegramChatId,
      parsedAt: parsed.date,
      updatedAt: new Date(),
      stops: {
        create: stops.map((s) => ({
          stopNumber: s.stopNumber,
          type: s.type,
          puNumber: s.puNumber,
          note: s.note,
          scheduledTime: new Date(s.scheduledTime || parsed.date),
          timezone: s.timezone,
          facilityCode: s.facilityCode,
          fullAddress: s.fullAddress,
          city: s.city,
          state: s.state,
          zip: s.zip,
        })),
      },
      penalties: {
        create: penalties.map((p) => ({ description: p.description, amount: p.amount })),
      },
    },
  });
}

export async function findLoadByTripId(tripId: string) {
  return prisma.load.findUnique({
    where: { tripId },
    include: { stops: true, penalties: true },
  });
}
