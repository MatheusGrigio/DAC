import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from '../../shared/components/button/button';
import { Alert } from '../../shared/components/alert/alert';

interface Cliente {
  cpf: string;
  nome: string;
  cidade: string;
  estado: string;
  saldo: string;
}

@Component({
  selector: 'app-clientes',
  imports: [FormsModule, Button, Alert],
  templateUrl: './clientes.html',
  styleUrl: './clientes.css',
})
export class Clientes {
  busca = '';
  filtro = '';
  clientes: Cliente[] = [];

  get clientesFiltrados(): Cliente[] {
    const termo = this.filtro.trim().toLocaleLowerCase('pt-BR');
    return this.clientes
      .filter(cliente => cliente.nome.toLocaleLowerCase('pt-BR').includes(termo)
        || cliente.cpf.includes(termo))
      .sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'));
  }
}
