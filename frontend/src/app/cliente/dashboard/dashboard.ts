import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-dashboard-cliente',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css']
})
export class DashboardClienteComponent {
  private authService = inject(AuthService);
  
  get usuario() {
    return this.authService.usuarioLogado();
  }
  
  logout(): void {
    this.authService.logout();
  }
}
