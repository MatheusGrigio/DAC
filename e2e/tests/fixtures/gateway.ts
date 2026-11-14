export async function gatewayReboot(): Promise<void> {
  const gateway = process.env.GATEWAY_URL ?? 'http://localhost:3000';
  const r = await fetch(`${gateway}/reboot`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: '{}',
  });
  if (!r.ok) throw new Error(`reboot ${r.status}`);
}