# Stage 1: Build Vite frontend application
FROM node:20-alpine AS builder
WORKDIR /app

# Install dependencies
COPY package.json package-lock.json ./
RUN npm ci

# Copy sources and build
COPY . .
RUN npm run build

# Stage 2: Serve via Nginx
FROM nginx:1.27-alpine
WORKDIR /usr/share/nginx/html

# Copy compiled SPA dist
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy Nginx reverse proxy and SPA configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
