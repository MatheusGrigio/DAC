import { Decimal } from 'decimal.js';
import { DateTime } from 'luxon';
import { MovimentacaoResponse } from '../models/extrato.model';

export type Sentido = 'entrada' | 'saida';

export interface MovimentoExtrato {
  dataHora: DateTime;
  operacao: string;
  descricao: string; 
  valor: Decimal;
  sentido: Sentido;
}

export interface DiaExtrato {
  data: DateTime;
  movimentos: MovimentoExtrato[];
  saldoFinal: Decimal; 
}

const ROTULOS: Record<string, string> = {
  DEPOSITO: 'Depósito',
  SAQUE: 'Saque',
  TRANSFERENCIA: 'Transferência',
};

export function sentidoDe(mov: MovimentacaoResponse, numeroConta: string): Sentido {
  if (mov.tipo === 'SAQUE') return 'saida';
  if (mov.tipo === 'TRANSFERENCIA' && mov.origem?.numeroConta === numeroConta) return 'saida';
  return 'entrada';
}

export function paraMovimento(mov: MovimentacaoResponse, numeroConta: string): MovimentoExtrato {
  const descricao =
    mov.tipo === 'TRANSFERENCIA' ? `${mov.origem?.nome ?? '?'} → ${mov.destino?.nome ?? '?'}` : '';
  return {
    dataHora: DateTime.fromISO(mov.dataHora),
    operacao: ROTULOS[mov.tipo] ?? mov.tipo,
    descricao,
    valor: new Decimal(mov.valor),
    sentido: sentidoDe(mov, numeroConta),
  };
}

export function montarLinhaDoTempo(
  numeroConta: string,
  saldoAbertura: Decimal,
  movimentacoes: MovimentacaoResponse[],
  inicio: DateTime,
  fim: DateTime,
): DiaExtrato[] {
  const porDia = new Map<string, MovimentoExtrato[]>();
  const ordenados = movimentacoes
    .map((m) => paraMovimento(m, numeroConta))
    .sort((a, b) => a.dataHora.toMillis() - b.dataHora.toMillis());

  for (const mov of ordenados) {
    const chave = mov.dataHora.toISODate() ?? '';
    porDia.set(chave, [...(porDia.get(chave) ?? []), mov]);
  }

  const dias: DiaExtrato[] = [];
  let saldo = saldoAbertura;
  const ultimo = fim.startOf('day');

  for (let dia = inicio.startOf('day'); dia <= ultimo; dia = dia.plus({ days: 1 })) {
    const movimentos = porDia.get(dia.toISODate() ?? '') ?? [];
    for (const mov of movimentos) {
      saldo = mov.sentido === 'entrada' ? saldo.plus(mov.valor) : saldo.minus(mov.valor);
    }
    dias.push({ data: dia, movimentos, saldoFinal: saldo });
  }
  return dias;
}
