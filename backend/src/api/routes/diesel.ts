import { Router } from "express";
import { prisma } from "../../lib/prisma";

export const dieselRouter = Router();

dieselRouter.get("/", async (_req, res) => {
  const list = await prisma.diesel.findMany({ orderBy: [{ year: "desc" }, { weekNumber: "desc" }] });
  res.json(list);
});

dieselRouter.get("/:id", async (req, res) => {
  const id = parseInt(req.params.id, 10);
  if (isNaN(id)) return res.status(400).json({ error: "Invalid id" });
  const item = await prisma.diesel.findUnique({ where: { id } });
  if (!item) return res.status(404).json({ error: "Not found" });
  res.json(item);
});

dieselRouter.post("/", async (req, res) => {
  const body = req.body as Record<string, unknown>;
  const item = await prisma.diesel.create({
    data: {
      weekNumber: Number(body.weekNumber),
      year: Number(body.year),
      weekLabel: (body.weekLabel as string) || "",
      weekStartDate: new Date((body.weekStartDate as string) || Date.now()),
      weekEndDate: new Date((body.weekEndDate as string) || Date.now()),
      totalAmount: Number(body.totalAmount) || 0,
      gallons: typeof body.gallons === "number" ? body.gallons : null,
      pricePerGallon: typeof body.pricePerGallon === "number" ? body.pricePerGallon : null,
      location: (body.location as string) || null,
      vendor: (body.vendor as string) || null,
      rawText: (body.rawText as string) || "",
      fileName: (body.fileName as string) || null,
    },
  });
  res.status(201).json(item);
});

dieselRouter.patch("/:id", async (req, res) => {
  const id = parseInt(req.params.id, 10);
  if (isNaN(id)) return res.status(400).json({ error: "Invalid id" });
  const item = await prisma.diesel.update({
    where: { id },
    data: req.body as Record<string, unknown>,
  });
  res.json(item);
});

dieselRouter.delete("/:id", async (req, res) => {
  const id = parseInt(req.params.id, 10);
  if (isNaN(id)) return res.status(400).json({ error: "Invalid id" });
  await prisma.diesel.delete({ where: { id } });
  res.status(204).send();
});
