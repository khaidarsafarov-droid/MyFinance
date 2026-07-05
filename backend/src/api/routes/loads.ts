import { Router } from "express";
import { prisma } from "../../lib/prisma";

export const loadsRouter = Router();

loadsRouter.get("/", async (_req, res) => {
  const loads = await prisma.load.findMany({
    orderBy: { parsedAt: "desc" },
    include: { stops: true, penalties: true },
  });
  res.json(loads);
});

loadsRouter.get("/:id", async (req, res) => {
  const load = await prisma.load.findUnique({
    where: { id: req.params.id },
    include: { stops: true, penalties: true },
  });
  if (!load) return res.status(404).json({ error: "Load not found" });
  res.json(load);
});

loadsRouter.post("/", async (req, res) => {
  const body = req.body as Record<string, unknown>;
  const id = (body.tripId as string) || (body.id as string);
  if (!id) return res.status(400).json({ error: "tripId required" });
  const load = await prisma.load.create({
    data: {
      id,
      tripId: id,
      date: new Date((body.date as string) || Date.now()),
      totalRate: Number(body.totalRate) || 0,
      totalMiles: Number(body.totalMiles) || 0,
      pointA: (body.pointA as string) || "",
      pointB: (body.pointB as string) || "",
      puCount: Number(body.puCount) || 0,
      delCount: Number(body.delCount) || 0,
      weekNumber: Number(body.weekNumber) || 1,
      year: Number(body.year) || new Date().getFullYear(),
      rawMessage: (body.rawMessage as string) || "",
    },
  });
  res.status(201).json(load);
});

loadsRouter.patch("/:id", async (req, res) => {
  const load = await prisma.load.update({
    where: { id: req.params.id },
    data: req.body as Record<string, unknown>,
  });
  res.json(load);
});

loadsRouter.delete("/:id", async (req, res) => {
  await prisma.load.delete({ where: { id: req.params.id } });
  res.status(204).send();
});
