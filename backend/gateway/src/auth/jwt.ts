import jwt from 'jsonwebtoken';
import { JWT_EXPIRES_IN } from '../config.js';
import { Perfil } from '../types/enums.js';
import type { GatewayUser } from '../types/fastify.js';

export type JwtClaims = {
  cpf: string;
  tipo: GatewayUser['tipo'];
  jti: string;
};

export function signAccessToken(secret: string, claims: JwtClaims): string {
  return jwt.sign({ cpf: claims.cpf, tipo: claims.tipo, jti: claims.jti }, secret, {
    expiresIn: JWT_EXPIRES_IN,
  });
}

export function tokenExpiry(token: string): number {
  const payload = jwt.decode(token);
  if (typeof payload === 'object' && payload !== null && typeof payload.exp === 'number') {
    return payload.exp;
  }
  return Math.floor(Date.now() / 1000) + 8 * 3600;
}

export function verifyAccessToken(secret: string, token: string): GatewayUser {
  const payload = jwt.verify(token, secret);
  if (typeof payload !== 'object' || payload === null) {
    throw new jwt.JsonWebTokenError('invalid token');
  }
  const cpf = typeof payload.cpf === 'string' ? payload.cpf : '';
  const tipo =
    payload.tipo === Perfil.CLIENTE || payload.tipo === Perfil.GERENTE ? payload.tipo : '';
  const jti = typeof payload.jti === 'string' ? payload.jti : '';
  const exp = typeof payload.exp === 'number' ? payload.exp : 0;
  if (!cpf || !tipo || !jti || !exp) {
    throw new jwt.JsonWebTokenError('invalid payload');
  }
  return { cpf, tipo, jti, exp };
}
