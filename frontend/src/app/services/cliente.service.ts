import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Decimal } from 'decimal.js';
import { Observable, map } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  ClienteDetalhe,
  ClienteDetalheResponse,
  ClienteLista,
  ClienteListaResponse,
} from '../shared/models';

@Injectable({
  providedIn: 'root',
})
export class ClienteService {
  private http = inject(HttpClient);
  private readonly API_GATEWAY = environment.apiUrl;

  listar(busca = ''): Observable<ClienteLista[]> {
    const params = busca ? { busca } : undefined;
    return this.http.get<ClienteListaResponse>(`${this.API_GATEWAY}/clientes`, { params }).pipe(
      map((r) =>
        r.clientes.map((c) => ({
          cpf: c.cpf,
          nome: c.nome,
          cidade: c.cidade,
          estado: c.estado,
          saldo: new Decimal(c.saldo),
        })),
      ),
    );
  }

  obter(cpf: string): Observable<ClienteDetalhe> {
    return this.http.get<ClienteDetalheResponse>(`${this.API_GATEWAY}/clientes/${cpf}`).pipe(
      map((r) => ({
        cpf: r.cpf,
        nome: r.nome,
        email: r.email,
        telefone: r.telefone,
        salario: new Decimal(r.salario),
        endereco: r.endereco,
        links: r._links ?? {},
      })),
    );
  }
}
