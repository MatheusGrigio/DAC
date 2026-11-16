import {
  Component,
  DestroyRef,
  OnInit,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { Decimal } from 'decimal.js';
import { last } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { ContaService } from '../services/conta.service';
import { Confirmacao } from '../shared/components/confirmacao/confirmacao';
import { Conta } from '../shared/models';
import { MoedaPipe } from '../shared/pipes/moeda.pipe';
import {
  centavosParaDecimal,
  formatarMoeda,
} from '../shared/utils/dinheiro';
import { mensagemDeErro } from '../shared/utils/erro-http';

@Component({
  selector: 'app-transferencia',
  standalone: true,
  imports: [RouterLink, MoedaPipe, Confirmacao],
  templateUrl: './transferencia.html',
  styleUrls: ['./transferencia.css'],
})
export class Transferencia implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly contaService = inject(ContaService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly confirmacao = viewChild.required(Confirmacao);

  protected readonly conta = this.contaService.conta;
  protected readonly contaDestino = signal('');
  protected readonly valor = signal(new Decimal(0));
  protected readonly valorTexto = signal('');
  protected readonly carregando = signal(true);
  protected readonly enviando = signal(false);
  protected readonly atualizandoSaldo = signal(false);
  protected readonly erro = signal('');
  protected readonly sucesso = signal('');

  protected readonly ocupado = computed(
    () => this.carregando() || this.enviando() || this.atualizandoSaldo(),
  );

  protected readonly destinoIgualOrigem = computed(() => {
    const origem = this.conta()?.numero ?? '';
    const destino = this.contaDestino();
    return destino.length === 4 && origem === destino;
  });

  protected readonly saldoInsuficiente = computed(() => {
    const conta = this.conta();
    return !!conta && this.valor().gt(conta.saldo);
  });

  protected readonly podeEnviar = computed(
  () =>
    !!this.conta() &&
    /^\d{4}$/.test(this.contaDestino()) &&
    this.valor().gt(0) &&
    !this.destinoIgualOrigem() &&
    !this.saldoInsuficiente() &&
    !this.ocupado(),
  );

  ngOnInit(): void {
    const cpf = this.authService.usuarioLogado()?.cpf;

    if (!cpf) {
      this.carregando.set(false);
      this.erro.set('Não foi possível identificar o cliente. Faça login novamente.');
      return;
    }

    this.contaService.carregar(cpf)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.carregando.set(false),
        error: (e) => {
          this.carregando.set(false);
          this.erro.set(
            mensagemDeErro(e, 'Não foi possível carregar sua conta.'),
          );
          this.contaService.limpar();
        },
      });
  }

  protected aoDigitarConta(evento: Event): void {
    const campo = evento.target as HTMLInputElement;
    const valor = campo.value.replace(/\D/g, '').slice(0, 4);

    this.contaDestino.set(valor);
    campo.value = valor;
  }

  protected aoDigitarValor(evento: Event): void {
    const campo = evento.target as HTMLInputElement;
    const valor = centavosParaDecimal(campo.value);

    this.valor.set(valor);
    this.valorTexto.set(valor.isZero() ? '' : formatarMoeda(valor));
    campo.value = this.valorTexto();
  }

  protected enviar(evento: Event): void {
    evento.preventDefault();

    const conta = this.conta();
    if (!conta || !this.podeEnviar()) return;

    const valor = this.valor();
    const destino = this.contaDestino();

    this.confirmacao().abrir({
      titulo: 'Confirmar transferência',
      mensagem: `Transferir ${formatarMoeda(valor)} para a conta ${destino}?`,
      rotulo: 'Transferir',
    }, () => this.transferir(conta, destino, valor));
  }

  private transferir(conta: Conta, contaDestino: string, valor: Decimal): void {
    const saldoEsperado = conta.saldo.minus(valor);

    this.erro.set('');
    this.sucesso.set('');
    this.enviando.set(true);

    this.contaService.transferir(conta.numero, contaDestino, valor)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.enviando.set(false);
          this.valor.set(new Decimal(0));
          this.valorTexto.set('');
          this.contaDestino.set('');
          this.atualizarSaldo(conta.cpfCliente, valor, saldoEsperado);
        },
        error: (e) => {
          this.enviando.set(false);
          this.erro.set(this.traduzirErro(e));
        },
      });
  }

  private atualizarSaldo(
    cpf: string,
    valor: Decimal,
    saldoEsperado: Decimal,
  ): void {
    this.atualizandoSaldo.set(true);

    this.contaService.aguardarSaldo(cpf, saldoEsperado)
      .pipe(last(), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (contaAtualizada) => {
          this.atualizandoSaldo.set(false);

          this.sucesso.set(
            contaAtualizada.saldo.equals(saldoEsperado)
              ? `Transferência de ${formatarMoeda(valor)} realizada com sucesso.`
              : `Transferência de ${formatarMoeda(valor)} registrada. O saldo ainda está sendo atualizado.`,
          );
        },
        error: () => {
          this.atualizandoSaldo.set(false);
          this.sucesso.set(
            `Transferência de ${formatarMoeda(valor)} registrada, mas não foi possível atualizar o saldo agora.`,
          );
        },
      });
  }

  private traduzirErro(e: unknown): string {
    const status = (e as { status?: number })?.status;

    if (status === 400) {
      return 'Informe conta de destino e valor válidos.';
    }
    if (status === 403) {
      return 'Você só pode transferir da sua própria conta.';
    }
    if (status === 404) {
      return 'Conta de destino não encontrada.';
    }
    if (status === 422) {
      return mensagemDeErro(e, 'Não foi possível realizar a transferência.');
    }
    if (status === 401) {
      return 'Sua sessão expirou. Faça login novamente.';
    }

    return mensagemDeErro(
      e,
      'Não foi possível confirmar a transferência. Consulte o extrato antes de tentar novamente.',
    );
  }
}