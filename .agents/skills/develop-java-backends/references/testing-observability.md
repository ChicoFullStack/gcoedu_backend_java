# Testes, qualidade, observabilidade e resiliência

## Sumário

1. Estratégia de testes
2. Ferramentas Spring
3. Qualidade e desempenho
4. Logs, métricas e traces
5. Resiliência

## 1. Estratégia de testes

Construir uma pirâmide orientada a risco:

- testes unitários rápidos para regras, validações e transformações;
- testes de slice para MVC, persistência ou serialização quando o isolamento ajudar;
- testes de integração para wiring, banco, segurança, broker e cache;
- testes de contrato para fronteiras com evolução independente;
- poucos testes end-to-end para jornadas críticas.

Nomear testes pelo comportamento e manter Arrange-Act-Assert claro. Testar resultado observável em vez de detalhes de implementação.

## 2. Ferramentas Spring

- JUnit 5 e AssertJ como base.
- Mockito para colaboradores externos ou efeitos difíceis; evitar mocks profundos.
- MockMvc para Spring MVC sem servidor real quando suficiente.
- WebTestClient para WebFlux ou testes HTTP compatíveis.
- REST Assured para testes HTTP de integração quando agregar legibilidade.
- Testcontainers para PostgreSQL, MySQL, MongoDB, Redis, Kafka ou RabbitMQ quando diferenças reais importarem.
- WireMock ou MockWebServer para controlar integrações HTTP.
- Pact ou alternativa adequada para consumer-driven contracts.

Evitar H2 como substituto automático de PostgreSQL/MySQL quando dialeto, locking, índices ou tipos importarem.

## 3. Qualidade e desempenho

- Executar formatter, compilador, testes e análise estática adotados pelo projeto.
- Usar JaCoCo como sinal de lacunas, não como objetivo isolado.
- Considerar ArchUnit para fronteiras arquiteturais.
- Medir benchmarks com JMH para código JVM isolado; não usar cronômetros informais.
- Fazer teste de carga com cenário, dados e metas realistas; observar saturação e recuperação.
- Investigar flakiness; não mascarar com retries ilimitados no CI.
- Testar concorrência e idempotência em fluxos com disputa real.

## 4. Logs, métricas e traces

- Emitir logs estruturados com nível correto, timestamp, serviço, ambiente, correlation ID e contexto seguro.
- Não registrar payload completo por padrão.
- Usar Micrometer e Actuator com endpoints expostos minimamente.
- Medir RED para serviços: rate, errors, duration; usar USE para recursos: utilization, saturation, errors.
- Definir métricas de negócio que permitam detectar falhas silenciosas.
- Propagar contexto com OpenTelemetry quando houver tracing distribuído.
- Evitar labels de alta cardinalidade, como user ID ou URL livre.
- Criar alertas acionáveis ligados a SLOs; evitar alertar sobre todo pico transitório.

## 5. Resiliência

- Definir timeout em toda chamada remota.
- Repetir apenas falhas transitórias e operações idempotentes, com limite, backoff exponencial e jitter.
- Usar circuit breaker para evitar pressão contínua sobre dependência degradada.
- Usar bulkhead para impedir que uma dependência esgote todos os recursos.
- Definir fallback apenas se produzir resultado semanticamente aceitável; não ocultar corrupção ou autorização falha.
- Expor readiness diferente de liveness e evitar reinícios em cascata por dependência externa temporariamente indisponível.
- Testar degradação, recuperação e comportamento após timeout.
