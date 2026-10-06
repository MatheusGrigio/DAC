import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Decimal } from 'decimal.js';
import { DateTime } from 'luxon';
import { Observable, map, take, takeWhile, tap, timer, switchMap } from 'rxjs';
import { environment } from '../../environments/environment';
import { Conta, ContaResponse, OperacaoResponse } from '../shared/models';

function paraConta(r: ContaResponse): Conta {
  return {
    numero: r.numero,
    cpfCliente: r.cpfCliente,
    cpfGerente: r.cpfGerente,
    saldo: new Decimal(r.saldo),
    dataCriacao: DateTime.fromISO(r.dataCriacao),
    links: r._links ?? {},
  };
}

@Injectable({
  providedIn: 'root',
})
export class ContaService {
  private http = inject(HttpClient);
  private readonly API_GATEWAY = environment.apiUrl;
  readonly conta = signal<Conta | null>(null);

  obterPorCpf(cpf: string): Observable<Conta> {
    return this.http
      .get<ContaResponse>(`${this.API_GATEWAY}/clientes/${cpf}/conta`)
      .pipe(map(paraConta));
  }

  carregar(cpf: string): Observable<Conta> {
    return this.obterPorCpf(cpf).pipe(tap((conta) => this.conta.set(conta)));
  }

  depositar(numero: string, valor: Decimal): Observable<OperacaoResponse> {
    return this.http.post<OperacaoResponse>(
      `${this.API_GATEWAY}/contas/${numero}/deposito`,
      { valor: valor.toFixed(2) },
    );
  }

  aguardarSaldo(cpf: string, esperado: Decimal): Observable<Conta> {
    return timer(0, 1000).pipe(
      switchMap(() => this.carregar(cpf)),
      takeWhile((conta) => !conta.saldo.equals(esperado), true),
      take(6),
    );
  }

  limpar(): void {
    this.conta.set(null);
  }
}
