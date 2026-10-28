import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { JobService } from './job.service';
import { JobResponse } from '../shared/models/job.model';
import { environment } from '../../environments/environment';

describe('JobService', () => {
  let service: JobService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        JobService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(JobService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve ser instanciado com sucesso', () => {
    expect(service).toBeTruthy();
  });

  it('deve consultar status do job', () => {
    const mockJob: JobResponse = {
      jobId: 'job-123',
      status: 'PENDENTE',
    };

    service.consultarStatus('job-123').subscribe((res) => {
      expect(res).toEqual(mockJob);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/jobs/job-123/status`);
    expect(req.request.method).toBe('GET');
    req.flush(mockJob);
  });

  it('deve obter resultado inline do job', () => {
    const mockData = { total: 10 };

    service.obterResultado<typeof mockData>('job-123').subscribe((res) => {
      expect(res).toEqual(mockData);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/jobs/job-123/result`);
    expect(req.request.method).toBe('GET');
    req.flush(mockData);
  });

  it('deve obter recurso por domínio e resourceId', () => {
    const mockResource = { cpf: '12345678901', nome: 'Cliente Teste' };

    service.obterRecurso<typeof mockResource>('clientes', '12345678901').subscribe((res) => {
      expect(res).toEqual(mockResource);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/clientes/12345678901`);
    expect(req.request.method).toBe('GET');
    req.flush(mockResource);
  });
});
