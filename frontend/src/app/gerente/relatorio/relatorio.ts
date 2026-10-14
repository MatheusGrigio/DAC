import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Button } from '../../shared/components/button/button';
import { Alert } from '../../shared/components/alert/alert';
import { RelatorioClienteLinha } from '../../shared/models';
import { CpfPipe, MoedaPipe } from '../../shared/pipes';

@Component({
  selector: 'app-relatorio',
  imports: [RouterLink, Button, Alert, CpfPipe, MoedaPipe],
  templateUrl: './relatorio.html',
  styleUrl: './relatorio.css',
})
export class Relatorio {
  clientes: RelatorioClienteLinha[] = [];

  get clientesOrdenados(): RelatorioClienteLinha[] {
    return [...this.clientes].sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'));
  }
}
