import { Router } from "express";
import { prisma } from "../../lib/prisma";

export const analyzeRouter = Router();

analyzeRouter.get("/history/:jobId", async (req, res) => {
  const job = await prisma.analysisJob.findUnique({
    where: { id: req.params.jobId },
  });
  if (!job) return res.status(404).json({ error: "Job not found" });
  res.json({
    status: job.status,
    processed: job.processed,
    found: job.found,
    totalMsgs: job.totalMsgs,
    error: job.error,
    startedAt: job.startedAt,
    finishedAt: job.finishedAt,
  });
});

analyzeRouter.post("/history", async (req, res) => {
  const { chatId } = req.body as { chatId?: string };
  if (!chatId) return res.status(400).json({ error: "chatId required" });
  const job = await prisma.analysisJob.create({
    data: { chatId, status: "pending" },
  });
  const { analyzeHistoryQueue } = await import("../../jobs/queue");
  await analyzeHistoryQueue.add({
    jobId: job.id,
    chatId,
    replyMsgChatId: chatId,
  });
  res.status(202).json(job);
});
