import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { Decimal } from 'decimal.js';
import { DateTime } from 'luxon';
import { environment } from '../../environments/environment';
import {
  AutocadastroRequest,
  Solicitacao,
  SolicitacoesListModel,
  SolicitacaoResponse,
  SolicitacaoListaResponse,
} from '../shared/models';

function paraSolicitacao(r: SolicitacaoResponse): Solicitacao {
  return {
    cpf: r.cpf,
    nome: r.nome,
    email: r.email,
    telefone: r.telefone,
    salario: new Decimal(r.salario),
    status: r.status,
    motivo: r.motivo ?? undefined,
    dataHoraProcessamento: r.dataHoraProcessamento
      ? DateTime.fromISO(r.dataHoraProcessamento)
      : null,
    _links: r._links,
  };
}

@Injectable({
  providedIn: 'root',
})
export class SolicitacaoService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/solicitacoes`;

  cadastrar(cadastro: AutocadastroRequest): Observable<SolicitacaoResponse> {
    return this.http.post<SolicitacaoResponse>(this.apiUrl, cadastro);
  }

  listar(status?: string): Observable<SolicitacoesListModel> {
    const params = status ? new HttpParams().set('status', status) : undefined;
    return this.http
      .get<SolicitacaoListaResponse>(this.apiUrl, { params })
      .pipe(
        map((res) => ({
          solicitacoes: (res.solicitacoes || []).map(paraSolicitacao),
        })),
      );
  }

  aprovar(cpf: string): Observable<{ jobId: string }> {
    return this.http.post<{ jobId: string }>(`${this.apiUrl}/${cpf}/aprovacao`, {});
  }

  rejeitar(cpf: string, motivo: string): Observable<SolicitacaoResponse> {
    return this.http.post<SolicitacaoResponse>(`${this.apiUrl}/${cpf}/rejeicao`, { motivo });
  }
}
