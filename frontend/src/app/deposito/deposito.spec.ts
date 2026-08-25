import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Deposito } from './deposito';

describe('Deposito', () => {
  let component: Deposito;
  let fixture: ComponentFixture<Deposito>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Deposito]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Deposito);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
