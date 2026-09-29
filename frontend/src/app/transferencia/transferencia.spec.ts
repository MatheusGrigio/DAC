import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Transferencia } from './transferencia';

describe('TransferenciaComponent', () => {
  let component: Transferencia;
  let fixture: ComponentFixture<Transferencia>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Transferencia],
    }).compileComponents();

    fixture = TestBed.createComponent(Transferencia);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
