import Bull from "bull";

const redisUrl = process.env.REDIS_URL || "redis://localhost:6379";

export const analyzeHistoryQueue = new Bull("analyze-history", redisUrl, {
  defaultJobOptions: { attempts: 1, removeOnComplete: 100 },
});
