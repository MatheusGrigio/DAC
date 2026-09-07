export type GatewayUser = {
  cpf: string;
  tipo: 'CLIENTE' | 'GERENTE';
  jti: string;
  exp: number;
};

declare module 'fastify' {
  interface FastifyRequest {
    user?: GatewayUser;
  }
}
