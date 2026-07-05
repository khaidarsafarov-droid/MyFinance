import { prisma } from "../../lib/prisma";
import { parseLoadWithGemini } from "../gemini/parseLoad";
import { createLoad, updateLoad } from "./loadService";

export type DuplicateResult = "new" | "updated" | "ignored";

export async function handleDuplicate(
  newTripId: string,
  newMsgDate: Date,
  newText: string,
  chatId: string,
  msgId: number
): Promise<DuplicateResult> {
  const existing = await prisma.load.findUnique({
    where: { tripId: newTripId },
  });

  if (!existing) return "new";

  const existingDate = new Date(existing.parsedAt);
  if (newMsgDate > existingDate) {
    const parsed = await parseLoadWithGemini(newText);
    if (!parsed) return "ignored";
    await updateLoad(newTripId, parsed, msgId, chatId);
    return "updated";
  }
  return "ignored";
}

export async function analyzeAndSave(
  text: string,
  chatId: string,
  msgId: number,
  msgDate: Date
): Promise<{ created: boolean }> {
  const parsed = await parseLoadWithGemini(text);
  if (!parsed) return { created: false };
  await createLoad(parsed, msgId, chatId);
  return { created: true };
}

export async function analyzeAndUpdate(
  text: string,
  tripId: string,
  chatId: string,
  msgId: number,
  msgDate: Date
): Promise<void> {
  const parsed = await parseLoadWithGemini(text);
  if (!parsed) return;
  await updateLoad(tripId, parsed, msgId, chatId);
}
