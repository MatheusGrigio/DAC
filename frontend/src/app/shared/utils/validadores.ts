import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { centavosParaDecimal } from './dinheiro';
import { soDigitos } from './mascara';

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
