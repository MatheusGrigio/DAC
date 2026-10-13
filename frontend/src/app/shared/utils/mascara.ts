import { formatarMoeda, centavosParaDecimal } from './dinheiro';

export type TipoMascara = 'cpf' | 'cep' | 'telefone' | 'moeda';

export function soDigitos(texto: string): string {
  return texto.replace(/\D/g, '');
}

// cpf
export function mascararCpf(texto: string): string {
  const d = soDigitos(texto).slice(0, 11);
  if (d.length <= 3) return d;
  if (d.length <= 6) return `${d.slice(0, 3)}.${d.slice(3)}`;
  if (d.length <= 9) return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6)}`;
  return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6, 9)}-${d.slice(9)}`;
}

// cep 
export function mascararCep(texto: string): string {
  const d = soDigitos(texto).slice(0, 8);
  return d.length <= 5 ? d : `${d.slice(0, 5)}-${d.slice(5)}`;
}

// telefone
export function mascararTelefone(texto: string): string {
  const d = soDigitos(texto).slice(0, 11);
  if (d.length === 0) return '';
  if (d.length <= 2) return `(${d}`;
  if (d.length <= 6) return `(${d.slice(0, 2)}) ${d.slice(2)}`;
  if (d.length <= 10) return `(${d.slice(0, 2)}) ${d.slice(2, 6)}-${d.slice(6)}`;
  return `(${d.slice(0, 2)}) ${d.slice(2, 7)}-${d.slice(7)}`;
}

// dinheiro
export function mascararMoeda(texto: string): string {
  const valor = centavosParaDecimal(texto);
  return valor.isZero() ? '' : formatarMoeda(valor);
}

export function aplicarMascara(tipo: TipoMascara, texto: string): string {
  switch (tipo) {
    case 'cpf':
      return mascararCpf(texto);
    case 'cep':
      return mascararCep(texto);
    case 'telefone':
      return mascararTelefone(texto);
    case 'moeda':
      return mascararMoeda(texto);
  }
}
