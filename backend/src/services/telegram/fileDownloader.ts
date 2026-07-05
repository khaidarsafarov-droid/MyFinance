import { TelegrafContext } from "telegraf";

export async function downloadTelegramFile(ctx: TelegrafContext): Promise<{ buffer: Buffer; fileName: string; mimeType: string } | null> {
  const doc = ctx.message && "document" in ctx.message ? ctx.message.document : null;
  const photo = ctx.message && "photo" in ctx.message ? ctx.message.photo : null;

  let fileId: string;
  let fileName = "upload";
  let mimeType = "application/octet-stream";

  if (doc) {
    fileId = doc.file_id;
    fileName = doc.file_name || fileName;
    mimeType = doc.mime_type || mimeType;
  } else if (photo && photo.length > 0) {
    fileId = photo[photo.length - 1].file_id;
    fileName = "photo.jpg";
    mimeType = "image/jpeg";
  } else {
    return null;
  }

  const fileLink = await ctx.telegram.getFileLink(fileId);
  const response = await fetch(fileLink.href);
  if (!response.ok) return null;
  const arrayBuffer = await response.arrayBuffer();
  const buffer = Buffer.from(arrayBuffer);
  return { buffer, fileName, mimeType };
}
