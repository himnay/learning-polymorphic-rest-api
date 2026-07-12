# Learning Polymorphic REST APIs

<img src="image/spring-logo.png" alt="Spring" width="80"/>

Designing REST endpoints whose payloads are **polymorphic** — one endpoint, many concrete
shapes (`CardPayment | UpiPayment | NetBankingPayment`) — done properly with Jackson subtype
handling, bean validation per subtype, and an OpenAPI contract that documents the
discriminator.

---

## Table of contents

1. 💡 [The problem](#1-the-problem)
2. 🔹 [Jackson polymorphism toolbox](#2-jackson-polymorphism-toolbox)
3. 🏗️ [Design chosen for this repo](#3-design-chosen-for-this-repo)
4. 🌐 [OpenAPI: oneOf + discriminator](#4-openapi-oneof--discriminator)
5. ⚠️ [Validation & error shape](#5-validation--error-shape)
6. 🔐 [Security note: why never enable default typing](#6-security-note-why-never-enable-default-typing)
7. 🏗️ [Module layout](#7-module-layout)
8. 🧪 [Running & testing](#8-running--testing)
9. 📚 [Further reading](#9-further-reading)

---

## 1. The problem

A `POST /payments` endpoint must accept several payment methods with different required
fields. Three naive designs all hurt:

| Anti-pattern | Pain |
|---|---|
| One mega-DTO with every field nullable | Validation becomes if-soup; the contract lies |
| Endpoint per subtype (`/payments/card`, `/payments/upi`) | Client switch statements; N endpoints to version |
| `Map<String,Object>` payloads | No contract at all |

The polymorphic answer: one endpoint, an explicit **discriminator field**, and the
framework resolves the concrete type:

```json
POST /api/v1/payments
{ "type": "CARD", "amount": 499.00, "cardNumber": "4111...", "cvv": "123" }
{ "type": "UPI",  "amount": 499.00, "vpa": "user@upi" }
```

```mermaid
flowchart LR
    REQ[JSON body with type field] --> J[Jackson @JsonTypeInfo resolution]
    J --> C[CardPaymentRequest]
    J --> U[UpiPaymentRequest]
    J --> N[NetBankingPaymentRequest]
    C & U & N --> V[Bean validation per subtype] --> H[Single @PostMapping handler<br/>sealed-interface switch]
```

## 2. Jackson polymorphism toolbox

| Annotation | Role |
|---|---|
| `@JsonTypeInfo(use = NAME, include = EXISTING_PROPERTY, property = "type", visible = true)` | Declares the discriminator; `EXISTING_PROPERTY` keeps `type` a real, validatable field |
| `@JsonSubTypes({@Type(value = CardPaymentRequest.class, name = "CARD"), ...})` | Maps discriminator values to classes |
| `@JsonTypeName("CARD")` | Alternative per-subtype naming |
| `PolymorphicTypeValidator` | Allow-list guard when type names come from data |

Modern Java pairing: make the parent a **sealed interface** — the compiler then guarantees
the `@JsonSubTypes` list and the business `switch` cover the same set:

```java
@JsonTypeInfo(use = Id.NAME, include = As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = CardPaymentRequest.class, name = "CARD"),
    @JsonSubTypes.Type(value = UpiPaymentRequest.class,  name = "UPI")
})
public sealed interface PaymentRequest permits CardPaymentRequest, UpiPaymentRequest {
    String type();
    BigDecimal amount();
}

public record CardPaymentRequest(String type, @NotNull @Positive BigDecimal amount,
        @CreditCardNumber String cardNumber, @Pattern(regexp = "\\d{3}") String cvv)
        implements PaymentRequest {}
```

Responses go polymorphic the same way — the discriminator is serialized back, so clients
can dispatch without sniffing fields.

## 3. Design chosen for this repo

- **Discriminator**: explicit `type` string enum, `EXISTING_PROPERTY` + `visible = true`
- **Sealed interface + records** for request/response hierarchies — exhaustiveness at compile time
- **Single controller endpoint**, service-level `switch (request)` pattern matching
- **Strategy pattern** underneath: `PaymentHandler<T extends PaymentRequest>` beans keyed by type for open/closed extension
- **No class names on the wire, ever** (`use = Id.CLASS` banned — see security note)

## 4. OpenAPI: oneOf + discriminator

Polymorphism is contract-first representable — springdoc generates this from the
annotations:

```yaml
PaymentRequest:
  oneOf:
    - $ref: '#/components/schemas/CardPaymentRequest'
    - $ref: '#/components/schemas/UpiPaymentRequest'
  discriminator:
    propertyName: type
    mapping:
      CARD: '#/components/schemas/CardPaymentRequest'
      UPI: '#/components/schemas/UpiPaymentRequest'
```

Generated clients (openapi-generator) then produce proper subtype hierarchies instead of
`Object`.

## 5. Validation & error shape

- Subtype-specific constraints live on the subtype record — `@Valid` cascades after Jackson resolves the type
- Unknown discriminator → Jackson `InvalidTypeIdException` → advice maps to `400` ProblemDetail listing allowed values
- Cross-field rules (`cvv` required only for cards) stay inside the card record — no global if-soup

## 6. Security note: why never enable default typing

Jackson's `enableDefaultTyping()` / `@JsonTypeInfo(use = Id.CLASS)` deserializes attacker
supplied class names — the root of a long CVE family (gadget-chain RCE). Rules this repo
follows:

- discriminator values are **logical names**, mapped through an explicit `@JsonSubTypes` allow-list
- never `Id.CLASS`/`Id.MINIMAL_CLASS` on internet-facing DTOs
- if dynamic typing is unavoidable, register a strict `PolymorphicTypeValidator`

## 7. Module layout

```
learning-polymorphic-rest-api/
├── pom.xml                                # super-pom parent, Java 25, Spring Boot 4
└── payment-api/
    ├── pom.xml                            # web + validation + springdoc, Boot 4 webmvc test slice
    └── src
        ├── main/java/com/org/learning/payment/
        │   ├── PaymentApiApplication.java # defaults to the `local` profile
        │   ├── web/
        │   │   ├── PaymentController.java        # single POST /api/v1/payments + GET /{id}
        │   │   ├── PaymentControllerAdvice.java  # 400 ProblemDetail (validation, unknown type), 404
        │   │   └── dto/
        │   │       ├── PaymentRequest.java           # sealed interface + @JsonTypeInfo/@JsonSubTypes
        │   │       ├── CardPaymentRequest.java       # "CARD"        — cardNumber (16 digits), cvv (3 digits)
        │   │       ├── UpiPaymentRequest.java        # "UPI"         — vpa (.+@.+)
        │   │       ├── NetBankingPaymentRequest.java # "NET_BANKING" — bankCode
        │   │       └── PaymentResponse.java          # id, type (echoed discriminator), amount, status
        │   ├── handler/                   # strategy: PaymentHandler<T> + three @Component impls
        │   ├── service/PaymentService.java       # exhaustive switch dispatch + in-memory store
        │   └── exception/PaymentNotFoundException.java
        ├── main/resources/                # application-local.yml / application-prod.yml, banner.txt
        └── test/java
            ├── unit/                      # @WebMvcTest subtype round-trips, error cases
            └── intg/                      # @SpringBootTest + MockMvc full flow
```

Roadmap (remaining): generated TypeScript client demo from the springdoc contract.

## 8. Running & testing

```bash
# run all tests (unit + integration)
mvn test -Dmaven.gitcommitid.skip=true

# start the API on :8080 (profile defaults to `local`)
mvn spring-boot:run -pl payment-api -Dmaven.gitcommitid.skip=true
```

Exercise the polymorphic endpoint:

```bash
# CARD → 201 {"id":"...","type":"CARD","amount":499.0,"status":"AUTHORIZED"}
curl -s -X POST localhost:8080/api/v1/payments -H 'Content-Type: application/json' \
  -d '{"type":"CARD","amount":499.00,"cardNumber":"4111111111111111","cvv":"123"}'

# UPI → 201 status COLLECT_REQUESTED
curl -s -X POST localhost:8080/api/v1/payments -H 'Content-Type: application/json' \
  -d '{"type":"UPI","amount":250.50,"vpa":"user@upi"}'

# NET_BANKING → 201 status REDIRECT_INITIATED
curl -s -X POST localhost:8080/api/v1/payments -H 'Content-Type: application/json' \
  -d '{"type":"NET_BANKING","amount":1200.00,"bankCode":"HDFC"}'

# unknown discriminator → 400 ProblemDetail listing allowed types
curl -s -X POST localhost:8080/api/v1/payments -H 'Content-Type: application/json' \
  -d '{"type":"CRYPTO","amount":42.00}'

# fetch by id (unknown id → 404 ProblemDetail)
curl -s localhost:8080/api/v1/payments/<id>
```

Swagger UI with the generated oneOf + discriminator contract: <http://localhost:8080/swagger-ui.html>
(raw spec at `/v3/api-docs`).

## 9. Further reading

- [Jackson polymorphic deserialization docs](https://github.com/FasterXML/jackson-docs/wiki/JacksonPolymorphicDeserialization)
- [OpenAPI 3 — oneOf & discriminator](https://swagger.io/docs/specification/v3_0/data-models/inheritance-and-polymorphism/)
- [Baeldung — inheritance with Jackson](https://www.baeldung.com/jackson-inheritance)
- [CVE history of Jackson default typing](https://cowtowncoder.medium.com/on-jackson-cves-dont-panic-here-is-what-you-need-to-know-54cd0d6e8062)
