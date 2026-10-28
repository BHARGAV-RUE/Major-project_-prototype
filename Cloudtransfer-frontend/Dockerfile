# Step 1: Build Vite React Production Bundle
FROM node:20-alpine AS builder
WORKDIR /app

COPY Cloudtransfer-frontend/package*.json ./
RUN npm ci

COPY Cloudtransfer-frontend/ .
RUN npm run build

# Step 2: Serve via Nginx
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html

# SPA Routing Fallback for React Router
RUN echo 'server { \
    listen 80; \
    location / { \
        root /usr/share/nginx/html; \
        index index.html index.htm; \
        try_files $uri $uri/ /index.html; \
    } \
}' > /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]