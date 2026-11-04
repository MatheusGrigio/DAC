import { Component, inject, OnInit, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { Button } from '../shared/components/button/button';
import { Alert } from '../shared/components/alert/alert';
import { GerenteListaResponse, GerenteResponse } from '../shared/models';
import { CpfPipe, TelefonePipe } from '../shared/pipes';
import { mensagemDeErro } from '../shared/utils/erro-http';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-gerente',
  imports: [RouterLink, Button, Alert, CpfPipe, TelefonePipe],
  templateUrl: './gerente.html',
  styleUrl: './gerente.css',
})
export class Gerente implements OnInit {
  private http = inject(HttpClient);

  gerentes = signal<GerenteResponse[]>([]);
  carregando = signal(true);
  erro = signal('');

  ngOnInit(): void {
    this.http.get<GerenteListaResponse>(`${environment.apiUrl}/gerentes`).subscribe({
      next: (res) => {
        const lista = [...(res.gerentes ?? [])].sort((a, b) =>
          a.nome.localeCompare(b.nome, 'pt-BR'),
        );
        this.gerentes.set(lista);
        this.carregando.set(false);
      },
      error: (err) => {
        this.erro.set(mensagemDeErro(err, 'Não foi possível carregar os gerentes.'));
        this.carregando.set(false);
      },
    });
  }
}
