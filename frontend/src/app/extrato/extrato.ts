import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Decimal } from 'decimal.js';
import { DateTime } from 'luxon';
import { AuthService } from '../services/auth.service';
import { ContaService } from '../services/conta.service';
import { DataHoraPipe } from '../shared/pipes/data-hora.pipe';
import { MoedaPipe } from '../shared/pipes/moeda.pipe';
import { DiaExtrato, montarLinhaDoTempo } from '../shared/utils/extrato';
import { mensagemDeErro } from '../shared/utils/erro-http';

const PADRAO_DIAS = 30;
const MAXIMO_DIAS = 365;
const FORMATO_INPUT = 'yyyy-MM-dd'; 

@Component({
  selector: 'app-extrato',
  imports: [ReactiveFormsModule, RouterLink, MoedaPipe, DataHoraPipe],
  templateUrl: './extrato.html',
  styleUrl: './extrato.css',
})
export class Extrato implements OnInit {
  private authService = inject(AuthService);
  private contaService = inject(ContaService);

  readonly hoje = DateTime.now().toFormat(FORMATO_INPUT);

  protected readonly filtro = new FormGroup({
    dataInicio: new FormControl(DateTime.now().minus({ days: PADRAO_DIAS }).toFormat(FORMATO_INPUT), {
      nonNullable: true,
      validators: Validators.required,
    }),
    dataFim: new FormControl(this.hoje, {
      nonNullable: true,
      validators: Validators.required,
    }),
  });
  
  protected readonly carregando = signal(false);
  protected readonly erro = signal('');
  protected readonly numeroConta = signal('');
  protected readonly saldoAbertura = signal(new Decimal(0));
  protected readonly dias = signal<DiaExtrato[] | null>(null);

  ngOnInit(): void {
    const cpf = this.authService.usuarioLogado()?.cpf;
    if (!cpf) {
      return;
    }
    this.contaService.garantir(cpf).subscribe({
      next: (conta) => {
        this.numeroConta.set(conta.numero);
        this.consultar();
      },
      error: (e) => this.erro.set(mensagemDeErro(e, 'Não foi possível carregar sua conta.')),
    });
  }

  private validarPeriodo(dataInicio: DateTime, dataFim: DateTime): string | null {
    if (!dataInicio.isValid || !dataFim.isValid) {
      return 'Informe as duas datas.';
    }
    if (dataFim < dataInicio) {
      return 'A data final não pode ser anterior à data inicial.';
    }
    if (dataFim.diff(dataInicio, 'days').days > MAXIMO_DIAS) {
      return 'O intervalo máximo é de 365 dias.';
    } 
    return null;
  }
  protected consultar(): void {
    const { dataInicio, dataFim } = this.filtro.getRawValue();
    const Inicio = DateTime.fromFormat(dataInicio, FORMATO_INPUT);
    const Fim = DateTime.fromFormat(dataFim, FORMATO_INPUT);

    const problema = this.validarPeriodo(Inicio, Fim);
    if (problema) {
      this.erro.set(problema);
      return;
    }

    this.erro.set('');
    this.carregando.set(true);

    this.contaService.extrato(this.numeroConta(), Inicio, Fim).subscribe({
      next: (extrato) => {
        const abertura = new Decimal(extrato.saldoAbertura);
        this.saldoAbertura.set(abertura);
        this.dias.set(
          montarLinhaDoTempo(this.numeroConta(), abertura, extrato.movimentacoes, Inicio, Fim),
        );
        this.carregando.set(false);
      },
      error: (e) => {
        this.carregando.set(false);
        this.erro.set(mensagemDeErro(e, 'Não foi possível consultar o extrato.'));
      },
    });
  }
}