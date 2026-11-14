const gateway = process.env.GATEWAY_URL ?? 'http://localhost:3000';

async function waitHealth(deadlineMs: number): Promise<void> {
  const end = Date.now() + deadlineMs;
  while (Date.now() < end) {
    try {
      const r = await fetch(`${gateway}/health`);
      if (r.ok) {
        const body = (await r.json()) as { status?: string };
        if (body.status === 'UP') return;
      }
    } catch {
      /* retry */
    }
    await new Promise((r) => setTimeout(r, 2000));
  }
  throw new Error(`Gateway ${gateway}/health não respondeu UP a tempo`);
}

async function reboot(): Promise<void> {
  const r = await fetch(`${gateway}/reboot`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: '{}',
  });
  if (!r.ok) {
    throw new Error(`POST /reboot falhou: ${r.status}`);
  }
}

export default async function globalSetup(): Promise<void> {
  await waitHealth(120_000);
  await reboot();
}