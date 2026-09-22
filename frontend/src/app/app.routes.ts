import { Routes } from '@angular/router';
import { Login } from './pages/login/login';
import { authGuard } from './auth/auth.guard';

export const routes: Routes = [
  { path: 'login', component: Login },
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
  
  { path: '**', redirectTo: '/login' }
];