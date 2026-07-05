import type { Context } from "telegraf";
import { downloadTelegramFile } from "../../services/telegram/fileDownloader";
import { classifyAndParseFile } from "../../services/gemini/classifyAndParseFile";
import { savePaycheck } from "../../services/db/paycheckService";
import { saveDiesel } from "../../services/db/dieselService";
import { sendPushNotification } from "../../services/fcm/pushService";

export async function fileHandler(ctx: Context): Promise<void> {
  const msg = ctx.message;
  if (!msg) return;
  const chatId = String(ctx.chat?.id);
  const msgId = "message_id" in msg ? msg.message_id : 0;
  const msgDate = new Date(("date" in msg ? msg.date : 0) * 1000);

  const fileData = await downloadTelegramFile(ctx);
  if (!fileData) {
    await ctx.reply("❌ Не удалось скачать файл.");
    return;
  }

  const result = await classifyAndParseFile(
    fileData.buffer,
    fileData.fileName,
    fileData.mimeType
  );

  if (result.type === "paycheck" && result.data) {
    await savePaycheck(
      result.data,
      "[from Telegram file]",
      fileData.fileName,
      msgId,
      chatId
    );
    await ctx.reply(
      `💰 Зарплата добавлена!\n📅 ${result.data.weekLabel}\n💵 Выплата: $${result.data.netAmount.toFixed(2)}`
    );
    await sendPushNotification({
      title: "💰 Зарплата добавлена",
      body: `${result.data.weekLabel} • $${result.data.netAmount.toFixed(2)}`,
      data: { screen: "finance", week: String(result.data.weekNumber) },
    });
    return;
  }

  if (result.type === "diesel" && result.data) {
    await saveDiesel(
      result.data,
      "[from Telegram file]",
      fileData.fileName,
      msgId,
      chatId
    );
    await ctx.reply(
      `⛽ Дизель добавлен!\n📅 ${result.data.weekLabel}\n💵 Расход: $${result.data.totalAmount.toFixed(2)}`
    );
    await sendPushNotification({
      title: "⛽ Дизель добавлен",
      body: `${result.data.weekLabel} • $${result.data.totalAmount.toFixed(2)}`,
      data: { screen: "finance", week: String(result.data.weekNumber) },
    });
    return;
  }

  await ctx.reply("❓ Не удалось определить тип документа. Попробуйте другой файл.");
}
