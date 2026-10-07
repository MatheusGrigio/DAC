import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Solicitacao, SolicitacoesListModel } from '../shared/models';

@Injectable({
  providedIn: 'root'
})
export class SolicitacaoService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:3000/solicitacoes';

  listar(status?: string): Observable<SolicitacoesListModel> {
    const params = status ? { status } : {};
    return this.http.get<SolicitacoesListModel>(this.apiUrl, { params });
  }

  aprovar(cpf: string): Observable<{ jobId: string }> {
    return this.http.post<{ jobId: string }>(`${this.apiUrl}/${cpf}/aprovacao`, {});
  }

  rejeitar(cpf: string, motivo: string): Observable<Solicitacao> {
    return this.http.post<Solicitacao>(`${this.apiUrl}/${cpf}/rejeicao`, { motivo });
  }
}
