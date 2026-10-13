import { Decimal } from 'decimal.js';

export function formatarMoeda(valor: Decimal.Value | null | undefined): string {
  if (valor === null || valor === undefined || valor === '') {
    return '';
  }
  const d = new Decimal(valor);
  const negativo = d.isNegative() && !d.isZero();
  const [inteiro, centavos] = d.abs().toFixed(2).split('.');
  const agrupado = inteiro.replace(/\B(?=(\d{3})+(?!\d))/g, '.');
  return `${negativo ? '-' : ''}R$ ${agrupado},${centavos}`;
}

export function centavosParaDecimal(texto: string): Decimal {
  const digitos = texto.replace(/\D/g, '').slice(0, 12);
  return new Decimal(digitos || '0').div(100);
}
