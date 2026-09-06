FROM nginx:alpine
# 构建时由 docker build 上下文 frontend/ 提供 dist（npm run build 产出）+ nginx.conf + certs/(自签 TLS)
COPY dist/ /usr/share/nginx/html/
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY certs/ /etc/nginx/certs/
EXPOSE 80 443
