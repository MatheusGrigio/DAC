import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { DashboardGerenteComponent } from './dashboard';

describe('DashboardGerenteComponent', () => {
  let component: DashboardGerenteComponent;
  let fixture: ComponentFixture<DashboardGerenteComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardGerenteComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardGerenteComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
