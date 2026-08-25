import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Gerente } from './gerente';

describe('Gerente', () => {
  let component: Gerente;
  let fixture: ComponentFixture<Gerente>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Gerente]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Gerente);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
