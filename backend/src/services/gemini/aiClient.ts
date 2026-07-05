/**
 * AI client: Cerebras (first) + Gemini (fallback).
 * App + (Cerebras + Gemini) + Bot
 */
import { cerebrasComplete } from "./cerebrasClient";
import { getTextModel } from "./geminiClient";

export async function aiGenerateText(prompt: string, system: string): Promise<string | null> {
  const cerebras = await cerebrasComplete(prompt, system);
  if (cerebras) return cerebras;

  if (!process.env.GEMINI_API_KEY) return null;
  try {
    const model = getTextModel();
    const result = await model.generateContent(`${system}\n\n---\n\n${prompt}`);
    const text = result.response.text().trim();
    return text || null;
  } catch (e) {
    console.warn("[Gemini] Fallback failed:", (e as Error).message);
    return null;
  }
}
