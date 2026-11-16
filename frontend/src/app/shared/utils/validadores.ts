import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { centavosParaDecimal } from './dinheiro';
import { soDigitos } from './mascara';

export function cpfValido(cpf: string): boolean {
  if (!/^\d{11}$/.test(cpf) || /^(\d)\1{10}$/.test(cpf)) return false;

  const base = cpf.slice(0, 9);
  const primeiroDigito = digitoCpf(base);
  const segundoDigito = digitoCpf(base + primeiroDigito);
  return cpf.slice(9) === `${primeiroDigito}${segundoDigito}`;
}

function digitoCpf(base: string): number {
  const soma = [...base].reduce(
    (total, digito, indice) => total + Number(digito) * (base.length + 1 - indice),
    0,
  );
  const resto = soma % 11;
  return resto < 2 ? 0 : 11 - resto;
}

export function comDigitos(quantidade: number): ValidatorFn {
  return (controle: AbstractControl): ValidationErrors | null => {
    const valor = String(controle.value ?? '');
    if (!valor) return null;
    return soDigitos(valor).length === quantidade ? null : { digitos: { esperado: quantidade } };
  };
}

export const telefoneValido: ValidatorFn = (controle) => {
  const valor = String(controle.value ?? '');
  if (!valor) return null;
  const n = soDigitos(valor).length;
  return n === 10 || n === 11 ? null : { telefone: true };
};

export const moedaMaiorQueZero: ValidatorFn = (controle) => {
  const valor = String(controle.value ?? '');
  if (!valor) return null;
  return centavosParaDecimal(valor).gt(0) ? null : { moeda: true };
};
