import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { Autocadastro } from './autocadastro';

describe('Autocadastro', () => {
  let component: Autocadastro;
  let fixture: ComponentFixture<Autocadastro>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Autocadastro],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(Autocadastro);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it.each([
    { valor: '11122233396', invalido: false },
    { valor: '11122233397', invalido: true },
    { valor: '00000000000', invalido: true },
  ])('valida os dígitos do CPF $valor', async ({ valor, invalido }) => {
    const tela: HTMLElement = fixture.nativeElement;
    const campo = tela.querySelector<HTMLInputElement>('#cpf')!;
    campo.value = valor;
    campo.dispatchEvent(new Event('input'));
    campo.dispatchEvent(new Event('blur'));
    await fixture.whenStable();

    expect(component.cpfInvalido).toBe(invalido);
    expect(tela.textContent?.includes('Informe um CPF válido.')).toBe(invalido);
  });
});
