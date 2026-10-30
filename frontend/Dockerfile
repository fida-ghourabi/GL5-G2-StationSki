# syntax=docker/dockerfile:1

# Build the Angular application with the Node version supported by package.json.
FROM node:24.21.0-bookworm-slim AS build

WORKDIR /app

# Install locked dependencies before copying application sources for layer caching.
COPY package.json package-lock.json ./
RUN npm ci

COPY angular.json tsconfig.json tsconfig.app.json ./
COPY src ./src
COPY public ./public

RUN npm run build

# Serve only the compiled static files with an unprivileged NGINX runtime.
FROM nginx/nginx-unprivileged:1.30.6-alpine-slim

COPY --from=build /app/dist/boreal/browser/ /usr/share/nginx/html/

# Angular routes fall back to index.html. Nginx also proxies API requests
# to the internal Kubernetes backend Service behind the public load balancer.
RUN printf '%s\n' \
    'server {' \
    '    listen 8080;' \
    '    server_name _;' \
    '    root /usr/share/nginx/html;' \
    '    index index.html;' \
    '    server_tokens off;' \
    '    location = /healthz {' \
    '        access_log off;' \
    '        default_type text/plain;' \
    '        return 200 "ok";' \
    '    }' \
    '    location ^~ /stationSki {' \
    '        proxy_pass http://backend.stationski.svc.cluster.local:8087;' \
    '        proxy_http_version 1.1;' \
    '        proxy_set_header Host $host;' \
    '        proxy_set_header X-Real-IP $remote_addr;' \
    '        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;' \
    '        proxy_set_header X-Forwarded-Proto $scheme;' \
    '    }' \
    '    location / {' \
    '        try_files $uri $uri/ /index.html;' \
    '    }' \
    '}' > /etc/nginx/conf.d/default.conf

EXPOSE 8080
