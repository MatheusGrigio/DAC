import { ChangeDetectorRef, Component, DestroyRef, inject, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Decimal } from 'decimal.js';
import { catchError, finalize, last, map, of, switchMap } from 'rxjs';
import { AuthService, ContaService } from '../services';
import { Confirmacao } from '../shared/components/confirmacao/confirmacao';
import { formatarMoeda } from '../shared/utils/dinheiro';
import { mensagemDeErro } from '../shared/utils/erro-http';

@Component({
  selector: 'app-transferencia',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, Confirmacao],
  templateUrl: './transferencia.html',
  styleUrls: ['./transferencia.css']
})
export class Transferencia {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private contaService = inject(ContaService);
  private destroyRef = inject(DestroyRef);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly confirmacao = viewChild.required(Confirmacao);

  transferenciaForm = this.fb.nonNullable.group({
    contaDestino: ['', [Validators.required, Validators.pattern(/^\d{4}$/)]],
    valor: ['', [Validators.required, Validators.pattern(/^\d+(?:[.,]\d{1,2})?$/)]],
  });
  mensagemFeedback = '';
  isError = false;
  enviando = false;

  realizarTransferencia(): void {
    if (this.enviando) return;

    if (this.transferenciaForm.invalid) {
      this.transferenciaForm.markAllAsTouched();
      return;
    }

    const cpf = this.authService.usuarioLogado()?.cpf;
    const { contaDestino, valor } = this.transferenciaForm.getRawValue();
    const valorDecimal = new Decimal(valor.replace(',', '.'));
    if (!cpf || !valorDecimal.gt(0)) {
      this.isError = true;
      this.mensagemFeedback = cpf ? 'O valor deve ser maior que zero.' : 'Faça login novamente.';
      return;
    }

    this.confirmacao().abrir({
      titulo: 'Confirmar transferência',
      mensagem: `Transferir ${formatarMoeda(valorDecimal)} para a conta ${contaDestino}?`,
      rotulo: 'Transferir',
    }, () => this.transferir(cpf, contaDestino, valorDecimal));
  }

  private transferir(cpf: string, contaDestino: string, valorDecimal: Decimal): void {
    this.enviando = true;
    this.mensagemFeedback = '';
    this.contaService.carregar(cpf).pipe(
      switchMap(conta => {
        const saldoEsperado = conta.saldo.minus(valorDecimal);
        return this.contaService.transferir(conta.numero, contaDestino, valorDecimal).pipe(
          switchMap(() => this.contaService.aguardarSaldo(cpf, saldoEsperado).pipe(
            last(),
            map(atualizada => atualizada.saldo.equals(saldoEsperado)),
            catchError(() => of(false)),
          )),
        );
      }),
      takeUntilDestroyed(this.destroyRef),
      finalize(() => {
        this.enviando = false;
        this.cdr.markForCheck();
      }),
    ).subscribe({
      next: saldoAtualizado => {
        this.isError = false;
        this.mensagemFeedback = saldoAtualizado
          ? 'Transferência realizada com sucesso.'
          : 'Transferência realizada. O saldo ainda não foi atualizado nesta tela.';
        this.transferenciaForm.reset();
      },
      error: erro => {
        this.isError = true;
        this.mensagemFeedback = mensagemDeErro(erro, 'Não foi possível confirmar a transferência. Consulte o extrato antes de tentar novamente.');
      },
    });
  }
}
