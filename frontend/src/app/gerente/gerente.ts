import { Component, inject, OnInit, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule, NgForm } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize, of, switchMap } from 'rxjs';
import { Button } from '../shared/components/button/button';
import { Alert } from '../shared/components/alert/alert';
import {
  GerenteAtualizacao,
  GerenteInput,
  GerenteListaResponse,
  GerenteResponse,
  JobResponse,
} from '../shared/models';
import { CpfPipe, TelefonePipe } from '../shared/pipes';
import { mensagemDeErro } from '../shared/utils/erro-http';
import { JobService } from '../services/job.service';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-gerente',
  imports: [FormsModule, RouterLink, Button, Alert, CpfPipe, TelefonePipe],
  templateUrl: './gerente.html',
  styleUrl: './gerente.css',
})
export class Gerente implements OnInit {
  private http = inject(HttpClient);
  private jobService = inject(JobService);
  private readonly apiUrl = `${environment.apiUrl}/gerentes`;

  gerentes = signal<GerenteResponse[]>([]);
  podeCriar = signal(false);
  carregando = signal(true);
  salvando = signal(false);
  processandoCpf = signal<string | null>(null);
  erro = signal('');
  sucesso = signal('');
  mostrarFormulario = signal(false);
  gerenteEditando = signal<GerenteResponse | null>(null);
  edicaoModel: GerenteAtualizacao = { nome: '', telefone: '' };

  ngOnInit(): void {
    this.carregarGerentes();
  }

  carregarGerentes(): void {
    this.carregando.set(true);
    this.http.get<GerenteListaResponse>(this.apiUrl).subscribe({
      next: (res) => {
        const lista = [...(res.gerentes ?? [])].sort((a, b) =>
          a.nome.localeCompare(b.nome, 'pt-BR'),
        );
        this.gerentes.set(lista);
        this.podeCriar.set(!!res._links?.['criacao']);
        this.carregando.set(false);
      },
      error: (err) => {
        this.erro.set(mensagemDeErro(err, 'Não foi possível carregar os gerentes.'));
        this.carregando.set(false);
      },
    });
  }

  inserir(formulario: NgForm): void {
    if (formulario.invalid || this.salvando()) return;
    const { nome, cpf, email, telefone, senha } = formulario.value as GerenteInput;
    this.salvando.set(true);
    this.erro.set('');
    this.sucesso.set('');

    this.http
      .post<JobResponse>(this.apiUrl, {
        nome: nome.trim(),
        cpf: cpf.trim(),
        email: email.trim(),
        telefone: telefone.trim(),
        senha,
      })
      .pipe(
        switchMap((job) => this.jobService.aguardarConclusao(job.jobId)),
        finalize(() => this.salvando.set(false)),
      )
      .subscribe({
        next: () => {
          this.sucesso.set('Gerente cadastrado com sucesso!');
          this.mostrarFormulario.set(false);
          formulario.resetForm();
          this.carregarGerentes();
        },
        error: (err) => {
          this.erro.set(mensagemDeErro(err, 'Não foi possível cadastrar o gerente.'));
        },
      });
  }

  iniciarEdicao(gerente: GerenteResponse): void {
    this.mostrarFormulario.set(false);
    this.gerenteEditando.set(gerente);
    this.edicaoModel = { nome: gerente.nome, telefone: gerente.telefone };
    this.erro.set('');
    this.sucesso.set('');
  }

  salvarEdicao(formulario: NgForm): void {
    const alvo = this.gerenteEditando();
    if (!alvo || formulario.invalid || this.salvando()) return;
    this.salvando.set(true);
    this.erro.set('');
    this.sucesso.set('');

    this.http
      .put<GerenteResponse>(`${this.apiUrl}/${alvo.cpf}`, {
        nome: this.edicaoModel.nome.trim(),
        telefone: this.edicaoModel.telefone.trim(),
      })
      .pipe(finalize(() => this.salvando.set(false)))
      .subscribe({
        next: () => {
          this.sucesso.set('Gerente atualizado com sucesso!');
          this.gerenteEditando.set(null);
          this.carregarGerentes();
        },
        error: (err) => {
          this.erro.set(mensagemDeErro(err, 'Não foi possível atualizar o gerente.'));
        },
      });
  }

  remover(gerente: GerenteResponse): void {
    if (!confirm(`Deseja realmente remover o gerente ${gerente.nome}?`)) return;
    this.processandoCpf.set(gerente.cpf);
    this.erro.set('');
    this.sucesso.set('');

    this.http
      .delete(`${this.apiUrl}/${gerente.cpf}`, { observe: 'response' })
      .pipe(
        switchMap((res) => {
          if (res.status === 202 && res.body) {
            return this.jobService.aguardarConclusao((res.body as JobResponse).jobId);
          }
          return of(null);
        }),
        finalize(() => this.processandoCpf.set(null)),
      )
      .subscribe({
        next: () => {
          this.sucesso.set('Gerente removido com sucesso!');
          this.carregarGerentes();
        },
        error: (err) => {
          this.erro.set(mensagemDeErro(err, 'Não foi possível remover o gerente.'));
        },
      });
  }
}
