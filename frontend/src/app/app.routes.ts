import { Routes } from '@angular/router';
import { Login } from './pages/login/login';
import { authGuard } from './auth/auth.guard';
import { Transferencia } from './transferencia/transferencia';

export const routes: Routes = [
  { path: 'login', component: Login },
  {
    path: 'autocadastro',
    loadComponent: () => import('./pages/autocadastro/autocadastro').then(m => m.Autocadastro)
  },
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  
  {
    path: 'cliente/dashboard',
    canActivate: [authGuard],
    data: { role: 'CLIENTE' },
    loadComponent: () => 
      import('./cliente/dashboard/dashboard')
        .then(m => m.DashboardClienteComponent)
  },
  
  {
    path: 'gerente/dashboard',
    canActivate: [authGuard],
    data: { role: 'GERENTE' },
    loadComponent: () => 
      import('./gerente/dashboard/dashboard')
        .then(m => m.DashboardGerenteComponent)
  },
  
  {
    path: 'gerente/clientes',
    canActivate: [authGuard],
    data: { role: 'GERENTE' },
    loadComponent: () => import('./gerente/clientes/clientes').then(m => m.Clientes)
  },

  {
    path: 'gerente/relatorio',
    canActivate: [authGuard],
    data: { role: 'GERENTE' },
    loadComponent: () => import('./gerente/relatorio/relatorio').then(m => m.Relatorio)
  },
  
  { 
    path: 'cliente/transferencia',
    component: Transferencia,
    canActivate: [authGuard],
    data: { role: 'CLIENTE' }
  },

  { path: 'transferencia', redirectTo: '/cliente/transferencia', pathMatch: 'full' },
  { path: '**', redirectTo: '/login' }
];
