import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { JobResponse } from '../shared/models';

@Injectable({
  providedIn: 'root',
})
export class RelatorioService {
  private http = inject(HttpClient);
  private readonly API_GATEWAY = environment.apiUrl;

  gerarClientes(): Observable<JobResponse> {
    return this.http.get<JobResponse>(`${this.API_GATEWAY}/relatorios/clientes`);
  }
}
