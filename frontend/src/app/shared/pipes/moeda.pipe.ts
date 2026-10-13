import { Pipe, PipeTransform } from '@angular/core';
import { Decimal } from 'decimal.js';
import { formatarMoeda } from '../utils/dinheiro';

@Pipe({ name: 'moeda' })
export class MoedaPipe implements PipeTransform {
  transform(valor: Decimal.Value | null | undefined): string {
    return formatarMoeda(valor);
  }
}
