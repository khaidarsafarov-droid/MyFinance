import { Router } from "express";
import { prisma } from "../../lib/prisma";

export const paychecksRouter = Router();

paychecksRouter.get("/", async (_req, res) => {
  const list = await prisma.paycheck.findMany({ orderBy: [{ year: "desc" }, { weekNumber: "desc" }] });
  res.json(list);
});

paychecksRouter.get("/:id", async (req, res) => {
  const id = parseInt(req.params.id, 10);
  if (isNaN(id)) return res.status(400).json({ error: "Invalid id" });
  const item = await prisma.paycheck.findUnique({ where: { id } });
  if (!item) return res.status(404).json({ error: "Not found" });
  res.json(item);
});

paychecksRouter.post("/", async (req, res) => {
  const body = req.body as Record<string, unknown>;
  const item = await prisma.paycheck.create({
    data: {
      weekNumber: Number(body.weekNumber),
      year: Number(body.year),
      weekLabel: (body.weekLabel as string) || "",
      weekStartDate: new Date((body.weekStartDate as string) || Date.now()),
      weekEndDate: new Date((body.weekEndDate as string) || Date.now()),
      driverName: (body.driverName as string) || null,
      grossAmount: typeof body.grossAmount === "number" ? body.grossAmount : null,
      netAmount: Number(body.netAmount) || 0,
      rawText: (body.rawText as string) || "",
      fileName: (body.fileName as string) || null,
    },
  });
  res.status(201).json(item);
});

paychecksRouter.patch("/:id", async (req, res) => {
  const id = parseInt(req.params.id, 10);
  if (isNaN(id)) return res.status(400).json({ error: "Invalid id" });
  const item = await prisma.paycheck.update({
    where: { id },
    data: req.body as Record<string, unknown>,
  });
  res.json(item);
});

paychecksRouter.delete("/:id", async (req, res) => {
  const id = parseInt(req.params.id, 10);
  if (isNaN(id)) return res.status(400).json({ error: "Invalid id" });
  await prisma.paycheck.delete({ where: { id } });
  res.status(204).send();
});
