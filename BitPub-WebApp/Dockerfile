# Autore: Luca Franzon 20054744
# Configurazione per la creazione dell'immagine Docker
# Build stage
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
ENV CI=false
RUN npm ci --legacy-peer-deps
COPY . .

RUN npm run build

# Serve stage
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
