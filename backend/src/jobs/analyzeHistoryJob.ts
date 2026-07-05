import { prisma } from "../lib/prisma";
import { createLoad } from "../services/db/loadService";
import { parseLoadWithGemini } from "../services/gemini/parseLoad";
import { sendPushNotification } from "../services/fcm/pushService";
import { analyzeHistoryQueue } from "./queue";

const isLoadLike = (text: string): boolean =>
  /Trip\s*ID|PU#|Total\s*Rate/i.test(text);

function extractTripId(text: string): string | null {
  const m = text.match(/Trip\s*ID[:\s]*([A-Z0-9\-]+)/i);
  return m ? m[1] : null;
}

analyzeHistoryQueue.process(async (job) => {
  const { jobId, chatId, replyMsgChatId } = job.data as {
    jobId: string;
    chatId: string;
    replyMsgChatId: string;
  };

  await prisma.analysisJob.update({
    where: { id: jobId },
    data: { status: "running" },
  });

  let processed = 0;
  let found = 0;
  let offset = 0;
  const limit = 100;

  for (let round = 0; round < 20; round++) {
    const loads = await prisma.load.findMany({
      where: { telegramChatId: chatId },
      orderBy: { parsedAt: "asc" },
      skip: offset,
      take: limit,
    });
    if (loads.length === 0) break;
    processed += loads.length;
    found = loads.length;
    offset += limit;
  }

  await prisma.analysisJob.update({
    where: { id: jobId },
    data: {
      status: "done",
      processed,
      found,
      totalMsgs: processed,
      finishedAt: new Date(),
    },
  });

  await sendPushNotification({
    title: "✅ Анализ истории завершён",
    body: `Обработано записей: ${processed}`,
    data: { screen: "home" },
  });

  return { processed, found };
});
