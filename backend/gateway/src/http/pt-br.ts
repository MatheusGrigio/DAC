const collator = new Intl.Collator('pt-BR', { sensitivity: 'base' });

export function compareNome(a: string, b: string): number {
  return collator.compare(a, b);
}

export function sortByNome<T extends { nome: string }>(items: T[]): T[] {
  return [...items].sort((left, right) => compareNome(left.nome, right.nome));
}
