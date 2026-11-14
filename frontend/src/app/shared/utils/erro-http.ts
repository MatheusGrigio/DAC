import { HttpErrorResponse } from '@angular/common/http';

export function mensagemDeErro(erro: unknown, padrao: string): string {
  const e = erro as HttpErrorResponse;
  if (e?.status === 0) {
    return 'Não foi possível falar com o servidor. Verifique se o sistema está no ar.';
  }
  const corpo = e?.error;
  const mensagem = corpo?.mensagem ?? corpo?.message;
  return typeof mensagem === 'string' && mensagem ? mensagem : padrao;
}
