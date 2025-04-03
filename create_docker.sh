docker run -d --name postgres-signing -e POSTGRES_USER=keycloak -e POSTGRES_PASSWORD=keycloak -e POSTGRES_DB=keycloak-db -p 5432:5432 postgres

docker run -d --name keycloak-signing -p 8086:8080 -p 8443:8443 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  -e KC_DB=postgres \
  -e KC_DB_URL_HOST=postgres \
  -e KC_DB_URL_DATABASE=keycloak-db \
  -e KC_DB_USERNAME=keycloak \
  -e KC_DB_PASSWORD=keycloak \
  --link postgres-signing:postgres \
  quay.io/keycloak/keycloak:latest start-dev

# http://localhost:8086/admin/master/console/
# admin/admin

