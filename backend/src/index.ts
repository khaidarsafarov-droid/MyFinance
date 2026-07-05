import "dotenv/config";
import express from "express";
import { createBot } from "./bot";
import { apiRouter } from "./api/router";
import { initFirebase } from "./services/fcm/pushService";
import "./jobs/analyzeHistoryJob";

const app = express();
app.use(express.json({ limit: "10mb" }));

app.use("/api", apiRouter);

app.get("/health", (_req, res) => {
  res.json({ status: "ok", service: "truckerload-backend" });
});

const bot = createBot();
if (bot) {
  const webhookPath = "/webhook";
  app.use(bot.webhookCallback(webhookPath));
  const serverUrl = process.env.SERVER_URL;
  if (serverUrl) {
    bot.telegram.setWebhook(`${serverUrl}${webhookPath}`).catch((e) => {
      console.warn("Webhook set failed:", e.message);
    });
  }
}

initFirebase();

const port = Number(process.env.PORT) || 3000;
app.listen(port, () => {
  console.log(`TruckerLoad backend listening on port ${port}`);
});
