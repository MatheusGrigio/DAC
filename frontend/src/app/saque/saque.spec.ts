import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Saque } from './saque';

describe('Saque', () => {
  let component: Saque;
  let fixture: ComponentFixture<Saque>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Saque]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Saque);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
