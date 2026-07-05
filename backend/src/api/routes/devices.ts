import { Router } from "express";
import { prisma } from "../../lib/prisma";

export const devicesRouter = Router();

devicesRouter.post("/register", async (req, res) => {
  const { fcmToken } = req.body as { fcmToken?: string };
  if (!fcmToken || typeof fcmToken !== "string") {
    return res.status(400).json({ error: "fcmToken required" });
  }
  const device = await prisma.device.upsert({
    where: { fcmToken },
    create: { fcmToken },
    update: {},
  });
  res.status(201).json(device);
});
