import { Pipe, PipeTransform } from '@angular/core';
import { DateTime } from 'luxon';

@Pipe({ name: 'dataHora' })
export class DataHoraPipe implements PipeTransform {
  transform(valor: DateTime | string | null | undefined, formato = 'dd/MM/yyyy HH:mm'): string {
    if (!valor) return '';
    const data = typeof valor === 'string' ? DateTime.fromISO(valor) : valor;
    return data.isValid ? data.toFormat(formato) : '';
  }
}
