export interface AutocadastroPayload {
  nome: string;
  cpf: string;
  email: string;
  telefone: string;
  salario: string;
  cep: string;
  logradouro: string;
  numero: string;
  complemento?: string;
  cidade: string;
  uf: string;
}

let seq = 0;

function suffix(): string {
  seq += 1;
  return `${Date.now()}${seq}`;
}

/** CPF de teste (11 dígitos); o back não valida DV. */
export function novoCpf(): string {
  const n = BigInt(suffix()) % BigInt(99_999_999_999n);
  return n.toString().padStart(11, '0');
}

export function novoEmail(prefixo = 'e2e'): string {
  return `${prefixo}${suffix()}@bantads.com.br`;
}

export function dadosAutocadastro(overrides: Partial<AutocadastroPayload> = {}): AutocadastroPayload {
  return {
    nome: 'Cliente E2E Teste',
    cpf: novoCpf(),
    email: novoEmail('cli'),
    telefone: '41999998888',
    salario: '3500,00',
    cep: '80010000',
    logradouro: 'Rua Teste E2E',
    numero: '100',
    complemento: '',
    cidade: 'Curitiba',
    uf: 'PR',
    ...overrides,
  };
}

export const SEED = {
  cliente: { email: 'cli1@bantads.com.br', senha: 'tads', cpf: '12912861012', conta: '1291' },
  cliente2: { email: 'cli2@bantads.com.br', senha: 'tads', conta: '0950' },
  cliente4: { email: 'cli4@bantads.com.br', senha: 'tads' },
  gerente: { email: 'ger1@bantads.com.br', senha: 'tads', cpf: '98574307084', nome: 'Geniéve' },
} as const;

export function dadosNovoGerente(): {
  nome: string;
  cpf: string;
  email: string;
  telefone: string;
  senha: string;
} {
  return {
    nome: 'Gerente E2E',
    cpf: novoCpf(),
    email: novoEmail('ger'),
    telefone: '41977776666',
    senha: 'SenhaE2E9!',
  };
}