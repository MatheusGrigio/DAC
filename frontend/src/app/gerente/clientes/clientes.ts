import { Component, inject, OnInit, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Decimal } from 'decimal.js';
import { Button } from '../../shared/components/button/button';
import { Alert } from '../../shared/components/alert/alert';
import { ClienteLista, ClienteListaResponse } from '../../shared/models';
import { CpfPipe, MoedaPipe } from '../../shared/pipes';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-clientes',
  imports: [FormsModule, RouterLink, Button, Alert, CpfPipe, MoedaPipe],
  templateUrl: './clientes.html',
  styleUrl: './clientes.css',
})
export class Clientes implements OnInit {
  private http = inject(HttpClient);

  busca = '';
  filtro = '';
  clientes = signal<ClienteLista[]>([]);
  carregando = signal(false);
  erro = signal('');

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    if (this.carregando()) return;
    this.carregando.set(true);
    this.erro.set('');
    this.http.get<ClienteListaResponse>(`${environment.apiUrl}/clientes`).subscribe({
      next: (response) => {
        this.clientes.set(
          response.clientes.map((cliente) => ({
            cpf: cliente.cpf,
            nome: cliente.nome,
            cidade: cliente.cidade,
            estado: cliente.estado,
            saldo: new Decimal(cliente.saldo),
          })),
        );
        this.carregando.set(false);
      },
      error: () => {
        this.erro.set('Não foi possível carregar os clientes. Tente novamente mais tarde.');
        this.carregando.set(false);
      },
    });
  }

  get clientesFiltrados(): ClienteLista[] {
    const termo = this.filtro.trim().toLocaleLowerCase('pt-BR');
    return this.clientes()
      .filter(
        (cliente) =>
          cliente.nome.toLocaleLowerCase('pt-BR').includes(termo) ||
          cliente.cpf.includes(termo),
      )
      .sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'));
  }
}
