import { Telegraf } from "telegraf";
import { messageHandler } from "./handlers/messageHandler";
import { fileHandler } from "./handlers/fileHandler";
import { historyHandler } from "./handlers/historyHandler";
import { analyzeHistoryQueue } from "../jobs/queue";
import { prisma } from "../lib/prisma";

export function createBot() {
  const token = process.env.BOT_TOKEN;
  if (!token) {
    console.warn("BOT_TOKEN not set, Telegram bot disabled");
    return null;
  }
  const bot = new Telegraf(token);

  bot.use(async (ctx, next) => {
    console.log(`[${ctx.chat?.id}] ${ctx.updateType}`);
    return next();
  });

  bot.command("start", (ctx) => {
    ctx.reply(
      "🚛 TruckerLoad Bot активен!\n\nОтправьте данные о грузе или файл зарплаты/дизеля."
    );
  });

  bot.command("analyze", async (ctx) => {
    const chatId = String(ctx.chat?.id);
    const job = await prisma.analysisJob.create({
      data: { chatId, status: "pending" },
    });
    await ctx.reply(
      `🔍 Анализ истории запущен. ID: ${job.id}\nПроверьте прогресс: GET /api/analyze/history/${job.id}`
    );
    await analyzeHistoryQueue.add({
      jobId: job.id,
      chatId,
      replyMsgChatId: chatId,
    });
  });

  bot.on("text", messageHandler);
  bot.on("document", fileHandler);
  bot.on("photo", fileHandler);

  return bot;
}
