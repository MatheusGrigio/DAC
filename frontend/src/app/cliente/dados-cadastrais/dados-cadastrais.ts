import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ClienteService } from '../../services/cliente.service';
import { ClienteDetalhe } from '../../shared/models';
import { CpfPipe, MoedaPipe, TelefonePipe } from '../../shared/pipes';
import { mensagemDeErro } from '../../shared/utils/erro-http';

@Component({
  selector: 'app-dados-cadastrais',
  imports: [RouterLink, CpfPipe, MoedaPipe, TelefonePipe],
  templateUrl: './dados-cadastrais.html',
  styleUrl: './dados-cadastrais.css',
})
export class DadosCadastrais implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly clienteService = inject(ClienteService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly cliente = signal<ClienteDetalhe | null>(null);
  protected readonly carregando = signal(true);
  protected readonly erro = signal('');

  ngOnInit(): void {
    const cpf = this.authService.usuarioLogado()?.cpf;
    if (!cpf) {
      this.carregando.set(false);
      this.erro.set('Não foi possível identificar o cliente. Faça login novamente.');
      return;
    }

    this.clienteService
      .obter(cpf)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (dados) => {
          this.cliente.set(dados);
          this.carregando.set(false);
        },
        error: (e) => {
          this.erro.set(mensagemDeErro(e, 'Não foi possível carregar seus dados cadastrais.'));
          this.carregando.set(false);
        },
      });
  }
}