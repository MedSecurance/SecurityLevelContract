# How to deploy
Deploy the service starting the docker compose:
```
docker compose -f docker-compose.yml -p signinginterface up -d
```

# How to use
Connect to the desired interface:
- for the provider's one: `localhost:8080` with the credentials `provider`/`provider`
- for the consumer's one: `localhost:8081` with the credentials `consumer`/`consumer`

Use either Generate buttons, to generate content to be signed. Then sign the content using the corresponding button. If the content has to be signed by both parties, upload the signed content from one to the other. Finally, the signature can be checked using the corresponding button.
