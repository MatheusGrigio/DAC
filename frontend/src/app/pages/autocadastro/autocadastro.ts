import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-autocadastro',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './autocadastro.html',
  styleUrl: './autocadastro.css',
})
export class Autocadastro {
  carregando = false;
  mensagemSucesso = '';
  mensagemErro = '';

  cadastro = {
    nome: '',
    cpf: '',
    email: '',
    telefone: '',
    salario: '',
    cep: '',
    logradouro: '',
    numero: '',
    complemento: '',
    cidade: 'araucity',
    uf: '',
  };

  somenteNumeros(evento: Event, campo: 'cpf' | 'telefone' | 'cep'): void {
    const input = evento.target as HTMLInputElement;
    const limite = campo === 'cpf' ? 11 : campo === 'cep' ? 8 : 11;
    const valor = input.value.replace(/\D/g, '').slice(0, limite);

    this.cadastro[campo] = valor;
    input.value = valor;
  }

  formatarSalario(evento: Event): void {
    const input = evento.target as HTMLInputElement;

    this.cadastro.salario = input.value
      .replace(/[^\d,.]/g, '')
      .replace(',', '.');

    input.value = this.cadastro.salario;
  }

  converterUfParaMaiusculo(): void {
    this.cadastro.uf = this.cadastro.uf
      .replace(/[^a-zA-Z]/g, '')
      .slice(0, 2)
      .toUpperCase();
  }
}