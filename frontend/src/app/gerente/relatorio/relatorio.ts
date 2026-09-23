import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Button } from '../../shared/components/button/button';
import { Alert } from '../../shared/components/alert/alert';

interface ClienteRelatorio {
  cpf: string;
  nome: string;
  email: string;
  salario: string;
  numeroConta: string;
  saldo: string;
  cpfGerente: string;
  nomeGerente: string;
}

@Component({
  selector: 'app-relatorio',
  imports: [RouterLink, Button, Alert],
  templateUrl: './relatorio.html',
  styleUrl: './relatorio.css',
})
export class Relatorio {
  clientes: ClienteRelatorio[] = [];

  get clientesOrdenados(): ClienteRelatorio[] {
    return [...this.clientes].sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'));
  }
}
