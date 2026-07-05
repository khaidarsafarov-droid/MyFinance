import type { Context } from "telegraf";
import { prisma } from "../../lib/prisma";
import { createLoad } from "../../services/db/loadService";
import { parseLoadWithGemini } from "../../services/gemini/parseLoad";
import { sendPushNotification } from "../../services/fcm/pushService";

const isLoadLike = (text: string): boolean =>
  /Trip\s*ID|PU#|Total\s*Rate/i.test(text);

function extractTripId(text: string): string | null {
  const m = text.match(/Trip\s*ID[:\s]*([A-Z0-9\-]+)/i);
  return m ? m[1] : null;
}

export async function historyHandler(ctx: Context): Promise<void> {
  const chatId = String(ctx.chat?.id);

  const job = await prisma.analysisJob.create({
    data: { chatId, status: "pending" },
  });

  await ctx.reply(
    `🔍 Анализ истории чата запущен.\nID задачи: ${job.id}\nИспользуйте REST: GET /api/analyze/history/${job.id} для прогресса.`
  );

  try {
    await prisma.analysisJob.update({
      where: { id: job.id },
      data: { status: "running" },
    });

    const messages: { id: number; text: string; date: number }[] = [];
    let offsetId = 0;
    const limit = 100;

    for (let i = 0; i < 50; i++) {
      const updates = await (ctx.telegram as unknown as { getUpdates?: (opts: unknown) => Promise<{ result: unknown[] }> }).getUpdates?.({ offset: offsetId, limit, timeout: 0 });
      if (!updates?.result?.length) break;
      const result = updates.result as { update_id: number; message?: { message_id: number; text?: string; date: number } }[];
      for (const u of result) {
        offsetId = u.update_id + 1;
        const m = u.message;
        if (m?.text && String(u.message?.chat?.id) === chatId) {
          messages.push({ id: m.message_id, text: m.text, date: m.date });
        }
      }
    }

    const sorted = [...messages].sort((a, b) => a.date - b.date);
    let processed = 0;
    let found = 0;

    for (const msg of sorted) {
      if (!isLoadLike(msg.text)) continue;
      processed++;
      const msgDate = new Date(msg.date * 1000);
      const tripId = extractTripId(msg.text);
      if (tripId) {
        const existing = await prisma.load.findUnique({ where: { tripId } });
        if (existing) {
          if (msgDate > new Date(existing.parsedAt)) {
            const parsed = await parseLoadWithGemini(msg.text);
            if (parsed) {
              const { updateLoad } = await import("../../services/db/loadService");
              await updateLoad(tripId, parsed, msg.id, chatId);
              found++;
            }
          }
          continue;
        }
      }
      const parsed = await parseLoadWithGemini(msg.text);
      if (parsed) {
        await createLoad(parsed, msg.id, chatId);
        found++;
      }
    }

    await prisma.analysisJob.update({
      where: { id: job.id },
      data: {
        status: "done",
        processed,
        found,
        totalMsgs: sorted.length,
        finishedAt: new Date(),
      },
    });

    await ctx.reply(
      `✅ Анализ завершён!\n📊 Обработано: ${processed}\n🚛 Найдено лоудов: ${found}`
    );
    await sendPushNotification({
      title: "✅ Анализ истории завершён",
      body: `Найдено ${found} лоудов`,
      data: { screen: "home" },
    });
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err);
    await prisma.analysisJob.update({
      where: { id: job.id },
      data: { status: "failed", error: message, finishedAt: new Date() },
    });
    await ctx.reply(`❌ Ошибка анализа: ${message}`);
  }
}
