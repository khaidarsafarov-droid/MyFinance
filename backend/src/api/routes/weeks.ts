import { Router } from "express";
import { prisma } from "../../lib/prisma";

export const weeksRouter = Router();

weeksRouter.get("/:year/:week", async (req, res) => {
  const year = parseInt(req.params.year, 10);
  const week = parseInt(req.params.week, 10);
  if (isNaN(year) || isNaN(week)) return res.status(400).json({ error: "Invalid year or week" });

  const [loads, paychecks, dieselList] = await Promise.all([
    prisma.load.findMany({ where: { year, weekNumber: week }, include: { stops: true, penalties: true } }),
    prisma.paycheck.findUnique({ where: { weekNumber_year: { weekNumber: week, year } } }),
    prisma.diesel.findMany({ where: { year, weekNumber: week } }),
  ]);

  const totalLoadRate = loads.reduce((s, l) => s + l.totalRate, 0);
  const totalMiles = loads.reduce((s, l) => s + l.totalMiles, 0);
  const paycheckAmount = paychecks?.netAmount ?? 0;
  const dieselAmount = dieselList.reduce((s, d) => s + d.totalAmount, 0);

  res.json({
    weekNumber: week,
    year,
    weekLabel: paychecks?.weekLabel ?? `Week ${week}, ${year}`,
    loadsCount: loads.length,
    totalLoadRate,
    totalMiles,
    paycheckAmount,
    hasPaycheck: !!paychecks,
    dieselAmount,
    hasDiesel: dieselList.length > 0,
    netProfit: paycheckAmount - dieselAmount,
    loads,
    paycheck: paychecks,
    diesel: dieselList,
  });
});
