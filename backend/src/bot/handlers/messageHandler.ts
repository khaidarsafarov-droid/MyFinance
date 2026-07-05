import type { Context } from "telegraf";
import { prisma } from "../../lib/prisma";
import { parseLoadWithGemini } from "../../services/gemini/parseLoad";
import { createLoad } from "../../services/db/loadService";
import { handleDuplicate, analyzeAndUpdate } from "../../services/db/duplicateService";
import { sendPushNotification } from "../../services/fcm/pushService";

const isLoadLike = (text: string): boolean =>
  /Trip\s*ID|Trip\nID|PU#|Total\s*Rate/i.test(text);

function extractTripId(text: string): string | null {
  const m = text.match(/Trip\s*ID[:\s]*([A-Z0-9\-]+)/i);
  return m ? m[1] : null;
}

export async function messageHandler(ctx: Context): Promise<void> {
  const msg = ctx.message;
  if (!msg || !("text" in msg)) return;
  const text = msg.text;
  const chatId = String(ctx.chat?.id);
  const msgId = msg.message_id;
  const msgDate = new Date((msg as { date: number }).date * 1000);

  if (!isLoadLike(text)) return;

  const tripId = extractTripId(text);
  if (tripId) {
    const result = await handleDuplicate(tripId, msgDate, text, chatId, msgId);
    if (result === "updated") {
      await ctx.reply(`🔄 Лоуд ${tripId} обновлён (найден более новый вариант)`);
      return;
    }
    if (result === "ignored") {
      return;
    }
  }

  const parsed = await parseLoadWithGemini(text);
  if (!parsed) {
    await ctx.reply("❌ Не удалось разобрать данные лоуда.");
    return;
  }

  const existing = await prisma.load.findUnique({ where: { tripId: parsed.tripId } });
  if (existing) {
    const existingDate = new Date(existing.parsedAt);
    if (msgDate > existingDate) {
      await analyzeAndUpdate(text, parsed.tripId, chatId, msgId, msgDate);
      await ctx.reply(`🔄 Лоуд ${parsed.tripId} обновлён`);
    }
    return;
  }

  await createLoad(parsed, msgId, chatId);
  await ctx.reply(
    `🚛 Лоуд добавлен!\n${parsed.tripId} • $${parsed.totalRate.toFixed(2)} • ${parsed.pointA} → ${parsed.pointB}`
  );
  await sendPushNotification({
    title: "🚛 Новый лоуд",
    body: `${parsed.tripId} • $${parsed.totalRate.toFixed(2)} • ${parsed.pointA} → ${parsed.pointB}`,
    data: { loadId: parsed.id, screen: "detail" },
  });
}
