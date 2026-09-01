# Notifieringstjänsten

Tredje tjänsten i pensionatets microservice-uppdelning. Tar emot en bokningsbekräftelse från
bokningstjänsten, hämtar kundens e-postadress från kundtjänsten och loggar bekräftelsen i sin egen
databas. Det finns ingen SMTP-server: raden i `notifications` *är* leveransbeviset, och samma text
skrivs till applikationsloggen.

Tjänsten äger bara sina notifieringar. Kunder ägs av kundtjänsten och bokningar av
bokningstjänsten, så `customerId` och `bookingId` lagras utan främmande nyckel och slås upp via
REST när de behövs.

## Kom igång

Kräver Docker och en JDK 21.

```bash
cat > .env <<'ENV'
JWT_SECRET=<openssl rand -base64 32>
ENV

docker compose up --build
```

Tjänsten på <http://localhost:8082>, hälsokoll på
<http://localhost:8082/actuator/health/readiness>.

`docker compose up` i det här repot startar tjänsten och dess databas, inget mer. Kundtjänsten
körs inte här, så anrop till `POST /api/notifications` svarar 503 tills den finns på
`CUSTOMER_SERVICE_URL`. Hela systemet startas från kundtjänstens repo, vars `docker-compose.yml`
bygger den här tjänsten från en syskonklon:

```bash
cd ../backend2_customer-service && docker compose up --build
```

`JWT_SECRET` måste vara identisk i alla tre tjänsterna, annars underkänns varandras tokens.

### Utveckling

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw spring-boot:run
```

Utan `SPRING_DATASOURCE_URL` går tjänsten mot `localhost:5434`. Compose-databasen publicerar ingen
port till värden, så starta en egen när du kör utanför Docker:

```bash
docker run --rm -p 5434:5432 \
  -e POSTGRES_DB=notificationdb -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres \
  postgres:17-alpine
```

### Tester

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw test
```

10 tester. Postgres startas av Testcontainers, så Docker måste vara igång.

`NotificationControllerIntegrationTest` gör riktiga HTTP-anrop genom hela kedjan: säkerhetsfilter,
controller, service, repository och databas. Bara kundtjänsten byts ut, och det sker på
Feign-gränssnittet, så klientens översättning av dess svar till 404 och 503 testas med.

## API

Alla endpoints kräver `Authorization: Bearer <token>`. Token hämtas från kundtjänstens
`POST /api/auth/login`.

| Metod | Path | Svar |
|---|---|---|
| POST | `/api/notifications` | 201 · 400 · 401 · 404 · 409 · 503 |
| GET | `/api/notifications?customerId=1` | 200 · 400 · 401 |

```bash
curl -X POST http://localhost:8082/api/notifications \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"customerId": 1, "bookingId": 42, "checkIn": "2026-09-10", "checkOut": "2026-09-12"}'
```

```json
{
  "id": 1,
  "customerId": 1,
  "bookingId": 42,
  "recipient": "anna@example.com",
  "message": "Hi Anna, your booking #42 is confirmed: 2026-09-10 to 2026-09-12. Welcome to Pensionatet.",
  "createdAt": "2026-09-01T11:51:25.709Z"
}
```

Fel returneras som `application/problem+json` med ett maskinläsbart `errorCode`:

```json
{
  "type": "/problems/customer-service-unavailable",
  "title": "Service is unavailable right now",
  "status": 503,
  "detail": "We could not look up the customer right now, so no confirmation was sent. Try again later",
  "instance": "/api/notifications",
  "errorCode": "CUSTOMER_SERVICE_UNAVAILABLE"
}
```

| Läge | Status | `errorCode` |
|---|---|---|
| Ogiltigt eller ofullständigt fält | 400 | `VALIDATION_FAILED` |
| Obegripligt datum eller saknad parameter | 400 | `INVALID_REQUEST` |
| Token saknas eller är ogiltig | 401 | (tom kropp) |
| Kunden finns inte | 404 | `CUSTOMER_NOT_FOUND` |
| Kunden saknar e-postadress | 409 | `CUSTOMER_HAS_NO_EMAIL` |
| Kundtjänsten svarar inte | 503 | `CUSTOMER_SERVICE_UNAVAILABLE` |

## Beroende till kundtjänsten

Innan en bekräftelse loggas hämtar tjänsten kunden:

```
GET {CUSTOMER_SERVICE_URL}/api/customers/{id}
Authorization: Bearer <anroparens token>
```

Feign-klienten har två sekunders connect- och read-timeout. En 404 betyder att kunden verkligen är
borta och att anroparen skickade fel id, så den förblir en 404. Allt annat, timeout, nekad
anslutning, 5xx eller oläsbar kropp, är vårt problem och blir en 503 med `Retry-After: 5`.
Ingenting sparas i något av felfallen, så anroparen kan göra om anropet utan att skapa dubbletter.

Tjänsten kraschar alltså inte när kundtjänsten är nere. Den svarar 503 och den som ringde får
försöka igen.

## JWT

Tjänsten utfärdar inga tokens, den validerar dem. Kundtjänsten signerar med HS256 och samma
`JWT_SECRET`, och `JwtConfig` kontrollerar signatur, utgångstid, `iss` och `aud`. En token som
saknar rätt `aud` avvisas även om signaturen stämmer.

Vid anrop vidare till kundtjänsten skickas anroparens token med. `FeignConfig` läser
`Authorization` från det inkommande anropet och sätter samma header på det utgående. Utan det
svarar kundtjänsten 401 och ingen bekräftelse skulle kunna skickas.

## Databas

Egen Postgres, ingen annan tjänst läser i den. Schemat skapas av Hibernate
(`spring.jpa.hibernate.ddl-auto=update`).

`notifications` innehåller `customerId`, `bookingId`, `recipient`, `message` och `createdAt`.
Rader uppdateras och raderas aldrig, tabellen är loggen. Mottagare och meddelandetext sparas som
de såg ut när bekräftelsen gick ut. Att läsa tillbaka dem från kundtjänsten i efterhand skulle
skriva om historiken så fort en kund byter e-postadress.

## Kubernetes

Manifesten för hela systemet ligger samlade i kundtjänstens repo under `k8s/`, inklusive
`notification-service.yaml` och `notification-db.yaml`. De använder imagen
`notification-service:latest`, som byggs av `docker compose build` i det repot.
