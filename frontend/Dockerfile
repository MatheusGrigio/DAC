# Build: Angular production bundle
FROM node:22-alpine AS build
WORKDIR /app

ARG API_URL=http://localhost:3000

COPY package.json package-lock.json ./
RUN npm ci

COPY angular.json tsconfig.json tsconfig.app.json ./
COPY public ./public
COPY src ./src

RUN sed -i "s|http://localhost:3000|${API_URL}|g" src/environments/environment.ts \
  && npm run build

# Runtime: nginx serving static files (SPA)
FROM nginx:1.27-alpine
RUN apk add --no-cache wget

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/hello-world/browser /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=10s --timeout=3s --retries=3 --start-period=5s \
  CMD wget -qO- http://127.0.0.1:80/ >/dev/null || exit 1