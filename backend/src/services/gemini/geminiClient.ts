import { GoogleGenerativeAI } from "@google/generative-ai";

const genAI = new GoogleGenerativeAI(process.env.GEMINI_API_KEY!);

export function getTextModel() {
  return genAI.getGenerativeModel({ model: "gemini-1.5-flash" });
}

export function getVisionModel() {
  return genAI.getGenerativeModel({ model: "gemini-1.5-flash" });
}
