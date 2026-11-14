import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Button } from '../../shared/components/button/button';
import { Alert } from '../../shared/components/alert/alert';
import { ClienteLista } from '../../shared/models';
import { CpfPipe, MoedaPipe } from '../../shared/pipes';
import { ClienteService } from '../../services/cliente.service';
import { mensagemDeErro } from '../../shared/utils/erro-http';

@Component({
  selector: 'app-clientes',
  imports: [FormsModule, RouterLink, Button, Alert, CpfPipe, MoedaPipe],
  templateUrl: './clientes.html',
  styleUrl: './clientes.css',
})
export class Clientes implements OnInit {
  private clienteService = inject(ClienteService);

  busca = '';
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
    this.clienteService.listar(this.busca.trim()).subscribe({
      next: (lista) => {
        this.clientes.set(lista);
        this.carregando.set(false);
      },
      error: (err) => {
        this.erro.set(mensagemDeErro(err, 'Não foi possível carregar os clientes. Tente novamente mais tarde.'));
        this.carregando.set(false);
      },
    });
  }

  buscar(): void {
    this.carregar();
  }
}