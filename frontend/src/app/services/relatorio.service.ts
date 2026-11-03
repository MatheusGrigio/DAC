import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { JobAceito } from '../shared/models';

@Injectable({
  providedIn: 'root',
})
export class RelatorioService {
  private http = inject(HttpClient);
  private readonly API_GATEWAY = environment.apiUrl;

  gerarClientes(): Observable<JobAceito> {
    return this.http.get<JobAceito>(`${this.API_GATEWAY}/relatorios/clientes`);
  }
}
