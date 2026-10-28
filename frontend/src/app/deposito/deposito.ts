import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Decimal } from 'decimal.js';
import { last } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { ContaService } from '../services/conta.service';
import { MoedaPipe } from '../shared/pipes/moeda.pipe';
import { centavosParaDecimal, formatarMoeda } from '../shared/utils/dinheiro';
import { mensagemDeErro } from '../shared/utils/erro-http';

@Component({
  selector: 'app-deposito',
  imports: [RouterLink, MoedaPipe],
  templateUrl: './deposito.html',
  styleUrl: './deposito.css',
})
export class Deposito implements OnInit {
  private authService = inject(AuthService);
  private contaService = inject(ContaService);

  protected readonly conta = this.contaService.conta;

  protected readonly valor = signal(new Decimal(0));
  protected readonly valorTexto = signal('');
  protected readonly enviando = signal(false);
  protected readonly atualizandoSaldo = signal(false);
  protected readonly erro = signal('');
  protected readonly sucesso = signal('');

  protected readonly podeEnviar = computed(
    () => !!this.conta() && this.valor().gt(0) && !this.enviando() && !this.atualizandoSaldo(),
  );

  ngOnInit(): void {
    const cpf = this.authService.usuarioLogado()?.cpf;
    if (cpf) {
      this.contaService.garantir(cpf).subscribe({
        error: (e) => this.erro.set(mensagemDeErro(e, 'Não foi possível carregar sua conta.')),
      });
    }
  }

  protected aoDigitar(evento: Event): void {
    const campo = evento.target as HTMLInputElement;
    const valor = centavosParaDecimal(campo.value);
    this.valor.set(valor);
    this.valorTexto.set(valor.isZero() ? '' : formatarMoeda(valor));
    campo.value = this.valorTexto();
  }

  protected enviar(evento: Event): void {
    evento.preventDefault();

    const conta = this.conta();
    if (!conta || !this.podeEnviar()) {
      return;
    }

    const valor = this.valor();
    const saldoEsperado = conta.saldo.plus(valor);

    this.erro.set('');
    this.sucesso.set('');
    this.enviando.set(true);

    this.contaService.depositar(conta.numero, valor).subscribe({
      next: () => {
        this.enviando.set(false);
        this.limparCampo();
        this.atualizarSaldo(conta.cpfCliente, valor, saldoEsperado);
      },
      error: (e) => {
        this.enviando.set(false);
        this.erro.set(this.traduzirErro(e));
      },
    });
  }

  private atualizarSaldo(cpf: string, valor: Decimal, saldoEsperado: Decimal): void {
    this.atualizandoSaldo.set(true);

    this.contaService.aguardarSaldo(cpf, saldoEsperado).pipe(last()).subscribe({
      next: (contaAtualizada) => {
        this.atualizandoSaldo.set(false);
        const refletiu = contaAtualizada.saldo.equals(saldoEsperado);
        this.sucesso.set(
          refletiu
            ? `Depósito de ${formatarMoeda(valor)} realizado com sucesso.`
            : `Depósito de ${formatarMoeda(valor)} registrado. O saldo pode levar alguns instantes para atualizar.`,
        );
      },
      error: () => {
        this.atualizandoSaldo.set(false);
        this.sucesso.set(
          `Depósito de ${formatarMoeda(valor)} registrado, mas não foi possível atualizar o saldo agora.`,
        );
      },
    });
  }

  private limparCampo(): void {
    this.valor.set(new Decimal(0));
    this.valorTexto.set('');
  }

  private traduzirErro(e: unknown): string {
    const status = (e as { status?: number })?.status;
    if (status === 400) {
      return 'Informe um valor válido para o depósito.';
    }
    if (status === 403) {
      return 'Você só pode depositar na sua própria conta.';
    }
    return mensagemDeErro(e, 'Não foi possível realizar o depósito. Tente novamente.');
  }
}
