import fs from 'node:fs';
import path from 'node:path';

const OUTBOX_DIR = path.resolve(process.cwd(), '..', 'outbox');

function safeFileName(to: string): string {
  return to.replace(/[<>:"/\\|?*]/g, '_');
}

export async function pollSenhaDoEmail(email: string, timeoutMs = 90_000): Promise<string> {
  const file = path.join(OUTBOX_DIR, `${safeFileName(email)}.txt`);
  const end = Date.now() + timeoutMs;
  while (Date.now() < end) {
    try {
      if (fs.existsSync(file)) {
        const content = fs.readFileSync(file, 'utf8');
        const match = content.match(/^senha:\s*(.+)$/m);
        if (match?.[1]) return match[1].trim();
      }
    } catch {
      /* retry */
    }
    await new Promise((r) => setTimeout(r, 750));
  }
  throw new Error(`Arquivo de senha não encontrado em outbox para ${email} (${file})`);
}