import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Decimal } from 'decimal.js';
import { Observable, map } from 'rxjs';
import { environment } from '../../environments/environment';
import { ClienteLista, ClienteListaResponse } from '../shared/models';

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
}
