import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule, NgForm } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Decimal } from 'decimal.js';
import { finalize } from 'rxjs';
import { SolicitacaoService } from '../../services/solicitacao.service';
import { Button } from '../../shared/components/button/button';
import { Alert } from '../../shared/components/alert/alert';
import { mensagemDeErro } from '../../shared/utils/erro-http';
import { cpfValido } from '../../shared/utils/validadores';

@Component({
  selector: 'app-autocadastro',
  standalone: true,
  imports: [FormsModule, RouterLink, Button, Alert],
  templateUrl: './autocadastro.html',
  styleUrl: './autocadastro.css',
})
export class Autocadastro {
  private solicitacaoService = inject(SolicitacaoService);
  private destroyRef = inject(DestroyRef);
  carregando = signal(false);
  mensagemSucesso = signal('');
  mensagemErro = signal('');

  cadastro = {
    nome: '',
    cpf: '',
    email: '',
    telefone: '',
    salario: '',
    cep: '',
    logradouro: '',
    numero: '',
    complemento: '',
    cidade: '',
    uf: '',
  };

  get cpfInvalido(): boolean {
    return !cpfValido(this.cadastro.cpf);
  }

  enviar(formulario: NgForm): void {
    if (formulario.invalid || this.cpfInvalido || this.carregando() || this.mensagemSucesso()) return;

    const { nome, cpf, email, telefone, salario, complemento, ...enderecoResto } = this.cadastro;
    const complementoLimpo = complemento.trim() || null;
    this.mensagemErro.set('');
    this.carregando.set(true);

    this.solicitacaoService.cadastrar({
      nome: nome.trim(),
      cpf,
      email: email.trim(),
      telefone,
      salario: new Decimal(salario).toFixed(2),
      endereco: {
        ...enderecoResto,
        complemento: complementoLimpo,
      },
    }).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.carregando.set(false)),
    ).subscribe({
      next: () => this.mensagemSucesso.set(
        'Solicitação enviada com sucesso! Aguarde a aprovação do gerente. Após a aprovação, você receberá sua senha por e-mail.',
      ),
      error: erro => this.mensagemErro.set(
        mensagemDeErro(erro, 'Não foi possível enviar a solicitação. Tente novamente.'),
      ),
    });
  }

  somenteNumeros(evento: Event, campo: 'cpf' | 'telefone' | 'cep'): void {
    const input = evento.target as HTMLInputElement;
    const limite = campo === 'cpf' ? 11 : campo === 'cep' ? 8 : 11;
    const valor = input.value.replace(/\D/g, '').slice(0, limite);

    this.cadastro[campo] = valor;
    input.value = valor;
  }

  formatarSalario(evento: Event): void {
    const input = evento.target as HTMLInputElement;

    this.cadastro.salario = input.value
      .replace(/[^\d,.]/g, '')
      .replace(',', '.');

    input.value = this.cadastro.salario;
  }
}
