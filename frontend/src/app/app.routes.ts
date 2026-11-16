import { Routes } from '@angular/router';
import { Login } from './pages/login/login';
import { authGuard } from './auth/auth.guard';

export const routes: Routes = [
  // PUBLIC ROUTES
  { 
    path: 'login', 
    component: Login 
  },
  {
    path: 'autocadastro',
    loadComponent: () => import('./pages/autocadastro/autocadastro').then(m => m.Autocadastro)
  },
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  
  // CLIENTE ROUTES
  {
    path: 'cliente/dashboard',
    canActivate: [authGuard],
    data: { role: 'CLIENTE' },
    loadComponent: () => 
      import('./cliente/dashboard/dashboard')
        .then(m => m.DashboardClienteComponent)
  },
  {
    path: 'cliente/deposito',
    canActivate: [authGuard],
    data: { role: 'CLIENTE' },
    loadComponent: () => import('./deposito/deposito').then(m => m.Deposito)
  },
  {
    path: 'cliente/saque',
    canActivate: [authGuard],
    data: { role: 'CLIENTE' },
    loadComponent: () => import('./saque/saque').then(m => m.Saque)
  },  
  { 
    path: 'cliente/transferencia',
    loadComponent: () => import('./transferencia/transferencia').then(m => m.Transferencia),
    canActivate: [authGuard],
    data: { role: 'CLIENTE' }
  },
  { 
    path: 'cliente/extrato',
    data: { role: 'CLIENTE' },
    canActivate: [authGuard],
    loadComponent: () => import('./extrato/extrato').then(m => m.Extrato)
  },
  {
    path: 'cliente/dados',
    canActivate: [authGuard],
    data: { role: 'CLIENTE' },
    loadComponent: () =>
      import('./cliente/dados-cadastrais/dados-cadastrais').then((m) => m.DadosCadastrais),
  },
  
  // GERENTE ROUTES
  {
    path: 'gerente',
    canActivate: [authGuard],
    data: { role: 'GERENTE' },
    loadComponent: () => import('./gerente/area/area').then((m) => m.GerenteArea),
    children: [
      { path: '', redirectTo: 'solicitacoes', pathMatch: 'full' },
      {
        path: 'solicitacoes',
        loadComponent: () =>
          import('./gerente/solicitacoes/solicitacoes').then((m) => m.GerenteSolicitacoes),
      },
      {
        path: 'clientes',
        loadComponent: () => import('./gerente/clientes/clientes').then((m) => m.Clientes),
      },
      {
        path: 'gerentes',
        loadComponent: () => import('./gerente/gerente').then((m) => m.Gerente),
      },
      {
        path: 'relatorio',
        loadComponent: () => import('./gerente/relatorio/relatorio').then((m) => m.Relatorio),
      },
    ],
  },
  {
    path: 'gerente/dashboard',
    redirectTo: '/gerente/solicitacoes',
    pathMatch: 'full',
  },
  { 
    path: 'transferencia', 
    redirectTo: '/cliente/transferencia', 
    pathMatch: 'full' 
  },
  { 
    path: '**', 
    redirectTo: '/login' 
  }
];
