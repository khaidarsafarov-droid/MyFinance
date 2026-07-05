/**
 * Cerebras Inference API — OpenAI-compatible.
 * App + (Cerebras + Gemini) + Bot
 */

const CEREBRAS_URL = "https://api.cerebras.ai/v1/chat/completions";
const MODEL = "llama3.1-8b";

export async function cerebrasComplete(
  prompt: string,
  system: string
): Promise<string | null> {
  const key = process.env.CEREBRAS_API_KEY;
  if (!key) return null;

  try {
    const res = await fetch(CEREBRAS_URL, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${key}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        model: MODEL,
        messages: [
          { role: "system", content: system },
          { role: "user", content: prompt },
        ],
        temperature: 0.2,
        max_tokens: 1024,
      }),
    });

    if (res.status < 200 || res.status >= 300) {
      console.warn("[Cerebras] Error", res.status, await res.text().then((t) => t.slice(0, 200)));
      return null;
    }

    const data = (await res.json()) as {
      choices?: Array<{ message?: { content?: string } }>;
    };
    const content = data.choices?.[0]?.message?.content?.trim();
    return content || null;
  } catch (e) {
    console.warn("[Cerebras] Request failed:", (e as Error).message);
    return null;
  }
}
