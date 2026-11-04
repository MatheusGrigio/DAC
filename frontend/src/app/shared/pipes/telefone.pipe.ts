import { Pipe, PipeTransform } from '@angular/core';
import { mascararTelefone } from '../utils/mascara';

@Pipe({ name: 'telefone' })
export class TelefonePipe implements PipeTransform {
  transform(valor: string | null | undefined): string {
    return valor ? mascararTelefone(valor) : '';
  }
}

