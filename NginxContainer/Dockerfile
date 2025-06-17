# Use official Nginx image as the base
FROM nginx:alpine

# Remove default configuration (optional but clean)
RUN rm /etc/nginx/conf.d/default.conf

# Copy custom reverse proxy config
COPY signer.conf /etc/nginx/conf.d/signer.conf

# Expose HTTP port
EXPOSE 80

# Start Nginx in foreground
CMD ["nginx", "-g", "daemon off;"]