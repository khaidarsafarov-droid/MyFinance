import { Router } from "express";
import { loadsRouter } from "./routes/loads";
import { paychecksRouter } from "./routes/paychecks";
import { dieselRouter } from "./routes/diesel";
import { weeksRouter } from "./routes/weeks";
import { devicesRouter } from "./routes/devices";
import { analyzeRouter } from "./routes/analyze";

export const apiRouter = Router();

apiRouter.use("/loads", loadsRouter);
apiRouter.use("/paychecks", paychecksRouter);
apiRouter.use("/diesel", dieselRouter);
apiRouter.use("/weeks", weeksRouter);
apiRouter.use("/devices", devicesRouter);
apiRouter.use("/analyze", analyzeRouter);
