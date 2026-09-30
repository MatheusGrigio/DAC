import { Component, inject, OnInit, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import Decimal from 'decimal.js';
import { Button } from '../../shared/components/button/button';
import { Alert } from '../../shared/components/alert/alert';

interface Cliente {
  cpf: string;
  nome: string;
  cidade: string;
  estado: string;
  saldo: Decimal;
}

@Component({
  selector: 'app-clientes',
  imports: [FormsModule, RouterLink, Button, Alert],
  templateUrl: './clientes.html',
  styleUrl: './clientes.css',
})
export class Clientes implements OnInit {
  private http = inject(HttpClient);

  busca = '';
  filtro = '';
  clientes = signal<Cliente[]>([]);
  carregando = signal(true);
  erro = signal('');

  ngOnInit(): void {
    this.http.get<{ clientes: (Omit<Cliente, 'saldo'> & { saldo: string })[] }>(
      'http://localhost:3000/clientes'
    ).subscribe({
      next: (response) => {
        this.clientes.set(response.clientes.map(cliente => ({
          ...cliente,
          saldo: new Decimal(cliente.saldo),
        })));
        this.carregando.set(false);
      },
      error: () => {
        this.erro.set('Não foi possível carregar os clientes. Tente novamente mais tarde.');
        this.carregando.set(false);
      },
    });
  }

  get clientesFiltrados(): Cliente[] {
    const termo = this.filtro.trim().toLocaleLowerCase('pt-BR');
    return this.clientes()
      .filter(cliente => cliente.nome.toLocaleLowerCase('pt-BR').includes(termo)
        || cliente.cpf.includes(termo))
      .sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'));
  }
}
