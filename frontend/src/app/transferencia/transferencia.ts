import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Decimal } from 'decimal.js'; 

@Component({
  selector: 'app-transferencia',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './transferencia.html',
  styleUrls: ['./transferencia.css']
})
export class Transferencia {
  transferenciaForm: FormGroup;
  mensagemFeedback: string = '';
  isError: boolean = false;

  constructor(private fb: FormBuilder) {
    this.transferenciaForm = this.fb.group({
      contaDestino: ['', [Validators.required, Validators.pattern('^[0-9]+$')]],
      valor: ['', [Validators.required, Validators.min(0.01)]]
    });
  }

  realizarTransferencia(): void {
    if (this.transferenciaForm.invalid) {
      this.transferenciaForm.markAllAsTouched();
      return;
    }

    try {
      const valorDecimal = new Decimal(this.transferenciaForm.value.valor);
      
      const payloadTransferencia = {
        contaDestino: this.transferenciaForm.value.contaDestino,
        valor: valorDecimal.toString() 
      };

      console.log('Enviando payload REST:', payloadTransferencia);
      
      this.isError = false;
      this.mensagemFeedback = 'Transferência solicitada com sucesso! Verifique seu extrato.';
      this.transferenciaForm.reset();

    } catch (error) {
      this.isError = true;
      this.mensagemFeedback = 'Erro ao processar o valor. Digite um formato válido.';
    }
  }
}