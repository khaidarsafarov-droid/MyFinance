import * as admin from "firebase-admin";

let initialized = false;

export function initFirebase() {
  if (initialized) return;
  const key = process.env.FIREBASE_PRIVATE_KEY;
  const clientEmail = process.env.FIREBASE_CLIENT_EMAIL;
  const projectId = process.env.FIREBASE_PROJECT_ID;
  if (!key || !clientEmail || !projectId) {
    console.warn("FCM: missing FIREBASE_* env, push disabled");
    return;
  }
  try {
    admin.initializeApp({
      credential: admin.credential.cert({
        projectId,
        clientEmail,
        privateKey: key.replace(/\\n/g, "\n"),
      }),
    });
    initialized = true;
  } catch (e) {
    console.warn("FCM init failed:", e);
  }
}

export interface PushPayload {
  title: string;
  body: string;
  data?: Record<string, string>;
}

export async function sendPushNotification(payload: PushPayload): Promise<void> {
  if (!initialized) return;
  const { prisma } = await import("../../lib/prisma");
  const devices = await prisma.device.findMany();
  const tokens = devices.map((d) => d.fcmToken);
  if (tokens.length === 0) return;
  const message: admin.messaging.MulticastMessage = {
    notification: { title: payload.title, body: payload.body },
    data: payload.data || {},
    tokens,
  };
  try {
    const res = await admin.messaging().sendEachForMulticast(message);
    res.responses.forEach((r, i) => {
      if (!r.success && r.error?.code === "messaging/invalid-registration-token") {
        prisma.device.deleteMany({ where: { fcmToken: tokens[i] } }).catch(() => {});
      }
    });
  } catch (e) {
    console.error("FCM send error:", e);
  }
}
