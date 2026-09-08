import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { CommandTypes } from '../src/types/command-types.ts';
import { EventType, ReplyStatus, SagaType } from '../src/types/enums.ts';
import {
  dinheiroSchema,
  eventTypeSchema,
  gerenteInputSchema,
  loginInputSchema,
  messageEnvelopeSchema,
  replyEnvelopeSchema,
  valorOperacaoSchema,
} from '../src/types/schemas.ts';

describe('shared contract types', () => {
  it('serializes command envelope without sagaId outside SAGA', () => {
    const envelope = {
      tipo: CommandTypes.CLIENTE_MARCAR_APROVADA,
      timestamp: '2026-04-30T10:00:00',
      payload: {},
    };
    const parsed = messageEnvelopeSchema.parse(envelope);
    const json = JSON.stringify(parsed);
    assert.equal(json.includes('sagaId'), false);
    assert.equal(parsed.tipo, 'cliente.marcar-aprovada');
  });

  it('keeps sagaId inside SAGA', () => {
    const parsed = messageEnvelopeSchema.parse({
      sagaId: '8f14e45f-ceea-4e1b-9d2a-52f7254b1f0b',
      tipo: CommandTypes.APROVAR_CLIENTE,
      timestamp: '2026-04-30T10:00:00',
      payload: { cpf: '12912861012' },
    });
    assert.equal(parsed.sagaId, '8f14e45f-ceea-4e1b-9d2a-52f7254b1f0b');
  });

  it('rejects unknown fields on envelopes (strict)', () => {
    assert.throws(() =>
      messageEnvelopeSchema.parse({
        tipo: CommandTypes.CLIENTE_CRIAR,
        timestamp: '2026-04-30T10:00:00',
        payload: {},
        extra: true,
      }),
    );
  });

  it('serializes reply with null erro', () => {
    const parsed = replyEnvelopeSchema.parse({
      sagaId: '8f14e45f-ceea-4e1b-9d2a-52f7254b1f0b',
      tipo: CommandTypes.CLIENTE_MARCAR_APROVADA,
      timestamp: '2026-04-30T10:00:00',
      status: ReplyStatus.SUCESSO,
      erro: null,
      payload: {},
    });
    const json = JSON.parse(JSON.stringify(parsed)) as { erro: null; status: string };
    assert.equal(json.erro, null);
    assert.equal(json.status, 'SUCESSO');
  });

  it('money is a two-decimal string not a number', () => {
    assert.equal(dinheiroSchema.parse('800.00'), '800.00');
    assert.throws(() => dinheiroSchema.parse('800'));
    assert.throws(() => dinheiroSchema.parse('800.0'));
    assert.throws(() => dinheiroSchema.parse(800));
    const op = valorOperacaoSchema.parse({ valor: '10.00' });
    assert.equal(JSON.stringify(op), '{"valor":"10.00"}');
  });

  it('login input requires email and senha only', () => {
    const parsed = loginInputSchema.parse({ email: 'cli1@bantads.com.br', senha: 'tads' });
    assert.equal(parsed.email, 'cli1@bantads.com.br');
    assert.throws(() =>
      loginInputSchema.parse({ email: 'cli1@bantads.com.br', senha: 'tads', extra: 1 }),
    );
  });

  it('event types keep exact accents', () => {
    assert.equal(eventTypeSchema.parse('Depósito'), EventType.DEPOSITO);
    assert.equal(eventTypeSchema.parse('TransferênciaOrigem'), EventType.TRANSFERENCIA_ORIGEM);
    assert.equal(SagaType.APROVAR_CLIENTE, 'aprovar-cliente');
  });

  it('GerenteInput requires senha and rejects extra fields', () => {
    const parsed = gerenteInputSchema.parse({
      cpf: '55667788990',
      nome: 'Gumercindo',
      email: 'ger5@bantads.com.br',
      telefone: '41988880005',
      senha: 'tads',
    });
    assert.equal(parsed.cpf, '55667788990');
    assert.throws(() =>
      gerenteInputSchema.parse({
        cpf: '55667788990',
        nome: 'Gumercindo',
        email: 'ger5@bantads.com.br',
        telefone: '41988880005',
        senha: 'tads',
        extra: true,
      }),
    );
  });
});
