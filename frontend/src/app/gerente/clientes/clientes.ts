import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from '../../shared/components/button/button';
import { Alert } from '../../shared/components/alert/alert';
import { ClienteLista } from '../../shared/models';
import { CpfPipe, MoedaPipe } from '../../shared/pipes';
import { ClienteService } from '../../services/cliente.service';
import { mensagemDeErro } from '../../shared/utils/erro-http';

interface FiltroClientes {
  termo: string;
  cidade: string;
  estado: string;
}

@Component({
  selector: 'app-clientes',
  imports: [FormsModule, Button, Alert, CpfPipe, MoedaPipe],
  templateUrl: './clientes.html',
  styleUrl: './clientes.css',
})
export class Clientes implements OnInit {
  private clienteService = inject(ClienteService);

  filtros: FiltroClientes = { termo: '', cidade: '', estado: '' };
  readonly clientesCarregados = signal<ClienteLista[]>([]);
  readonly carregando = signal(false);
  readonly erro = signal('');

  readonly clientes = computed(() => {
    const cidade = this.filtros.cidade.trim().toLocaleLowerCase('pt-BR');
    const estado = this.filtros.estado.trim().toUpperCase();
    return this.clientesCarregados().filter((cliente) => {
      const okCidade = !cidade || cliente.cidade.toLocaleLowerCase('pt-BR').includes(cidade);
      const okEstado = !estado || cliente.estado.toUpperCase().includes(estado);
      return okCidade && okEstado;
    });
  });

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    if (this.carregando()) return;
    this.carregando.set(true);
    this.erro.set('');
    const busca = this.termoParaApi(this.filtros.termo);
    this.clienteService.listar(busca).subscribe({
      next: (lista) => {
        this.clientesCarregados.set(lista);
        this.carregando.set(false);
      },
      error: (err) => {
        this.erro.set(
          mensagemDeErro(err, 'Não foi possível carregar os clientes. Tente novamente mais tarde.'),
        );
        this.carregando.set(false);
      },
    });
  }

  buscar(): void {
    this.carregar();
  }

  limparFiltros(): void {
    this.filtros = { termo: '', cidade: '', estado: '' };
    this.carregar();
  }

  private termoParaApi(termo: string): string {
    const bruto = termo.trim();
    if (!bruto) return '';
    const apenasDigitos = bruto.replace(/\D/g, '');
    if (apenasDigitos.length >= 3 && /^[\d.\-\s]+$/.test(bruto)) {
      return apenasDigitos;
    }
    return bruto;
  }
}