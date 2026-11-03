import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const erroInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  return next(req).pipe(
    catchError((erro: HttpErrorResponse) => {
      const rotaDeAutenticacao = req.url.endsWith('/login') || req.url.endsWith('/logout');
      if (erro.status === 401 && !rotaDeAutenticacao && authService.isAuthenticated()) {
        authService.encerrarSessaoLocal();
      }
      return throwError(() => erro);
    }),
  );
};
