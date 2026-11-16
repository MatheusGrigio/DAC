import { Component, Input, inject } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-button',
  imports: [],
  templateUrl: './button.html',
  styleUrl: './button.css',
})
export class Button {
  private router = inject(Router);

  @Input() type: 'button' | 'submit' = 'button';
  @Input() variant: 'primary' | 'secondary' | 'danger' = 'primary';
  @Input() disabled = false;
  @Input() routerLink?: string | readonly string[];

  onClick(event: MouseEvent): void {
    if (!this.routerLink || this.disabled) return;
    event.preventDefault();
    const path = Array.isArray(this.routerLink) ? this.routerLink : [this.routerLink];
    void this.router.navigate(path);
  }
}