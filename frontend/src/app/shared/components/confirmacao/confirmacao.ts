import { Component, ElementRef, signal, viewChild } from '@angular/core';

type OpcoesConfirmacao = {
  titulo: string;
  mensagem: string;
  rotulo: string;
  perigo?: boolean;
  rotuloMotivo?: string;
};

@Component({
  selector: 'app-confirmacao',
  templateUrl: './confirmacao.html',
  styleUrl: './confirmacao.css',
})
export class Confirmacao {
  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');
  private acao?: (motivo: string) => void;
  protected readonly opcoes = signal<OpcoesConfirmacao | null>(null);
  protected readonly motivo = signal('');

  abrir(opcoes: OpcoesConfirmacao, acao: (motivo: string) => void): void {
    this.opcoes.set(opcoes);
    this.motivo.set('');
    this.acao = acao;
    this.dialogo().nativeElement.showModal();
  }

  protected cancelar(): void {
    this.acao = undefined;
    this.dialogo().nativeElement.close();
  }

  protected confirmar(evento: Event): void {
    evento.preventDefault();
    const motivo = this.motivo().trim();
    if (this.opcoes()?.rotuloMotivo && !motivo) return;

    const acao = this.acao;
    this.cancelar();
    acao?.(motivo);
  }
}
