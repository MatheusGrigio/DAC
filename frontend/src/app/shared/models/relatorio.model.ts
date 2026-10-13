export interface RelatorioClienteLinha {
  cpf: string;
  nome: string;
  email: string;
  salario: string;
  numeroConta: string;
  saldo: string;
  cpfGerente: string;
  nomeGerente: string;
}

export interface RelatorioClientes {
  clientes: RelatorioClienteLinha[];
}
