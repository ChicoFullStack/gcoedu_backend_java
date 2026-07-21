# APIs, frontend, dados e mensageria

## Sumário

1. Contratos HTTP
2. Integração frontend
3. GraphQL e WebSocket
4. Bancos e Redis
5. Kafka, RabbitMQ e eventos

## 1. Contratos HTTP

- Usar JSON consistente e datas em ISO 8601 com timezone definido.
- Adotar formato de erro estável, preferencialmente baseado em Problem Details quando adequado, com código de negócio, mensagem segura, campos inválidos e correlation ID.
- Distinguir 400, 401, 403, 404, 409, 422, 429 e 5xx corretamente.
- Exigir `Idempotency-Key` ou chave de negócio em comandos sujeitos a repetição, como pagamentos e criação remota.
- Aplicar limites de paginação e payload; não permitir filtros ou ordenações arbitrárias diretamente em SQL.
- Evoluir contratos de forma aditiva quando possível e publicar deprecação antes da remoção.

## 2. Integração frontend

- Definir uma origem canônica da API por ambiente e evitar URLs duplicadas no frontend.
- Em Axios, usar instância central, timeout, interceptador de autenticação e tratamento coerente de erros.
- Evitar loops de renovação: permitir uma tentativa de refresh, coordenar requisições concorrentes e encerrar a sessão se o refresh falhar.
- Em Fetch, verificar `response.ok`, cancelar com `AbortController` e tratar timeout explicitamente.
- Não expor tokens long-lived a JavaScript quando cookies seguros puderem atender ao modelo de ameaça.
- Se usar bearer token no cliente, definir armazenamento e renovação com base em threat model; não assumir que `localStorage` é seguro por padrão.
- Configurar CORS no backend para origens exatas por ambiente. Preflight deve refletir apenas métodos e headers necessários.
- Gerar tipos do OpenAPI quando isso reduzir divergência, mantendo a especificação validada no CI.

## 3. GraphQL e WebSocket

Para GraphQL:

- Autorizar resolver e objeto, não apenas o endpoint único.
- Usar DataLoader ou batch loading contra N+1.
- Limitar profundidade, complexidade, tamanho e tempo de execução.
- Desabilitar introspecção em produção somente se o ecossistema e a operação permitirem; não tratá-la como controle principal.

Para WebSocket:

- Autenticar handshake ou protocolo de aplicação e revalidar autorização em canais.
- Definir schema, tamanho máximo, heartbeat, expiração, reconexão e política de backpressure.
- Não confiar em identificadores de usuário enviados pelo cliente.
- Considerar afinidade, broker ou pub/sub ao escalar horizontalmente.

## 4. Bancos e Redis

- PostgreSQL/MySQL: usar constraints, índices baseados em queries reais, queries parametrizadas e análise de plano antes de otimizar.
- MongoDB: modelar pela forma de acesso, limitar documentos, criar índices e evitar transações distribuídas quando o modelo puder agregar dados.
- Redis: definir finalidade de cada chave, prefixo, TTL, serialização, máximo de memória e comportamento de falha.
- Não tratar cache como fonte de verdade salvo design explícito.
- Executar Flyway ou Liquibase de modo controlado; não editar migrations já aplicadas em ambientes compartilhados.
- Testar migrations com volume e versão representativos quando houver mudança arriscada.

## 5. Kafka, RabbitMQ e eventos

- Definir semântica de entrega e projetar consumidores idempotentes; “exactly once” tem limites de escopo.
- Versionar schemas de forma compatível e registrar ownership.
- Incluir event ID, timestamp, tipo, versão, correlation/causation IDs e chave adequada.
- Usar transactional outbox quando for necessário coordenar alteração no banco e publicação de evento.
- Limitar retries, aplicar backoff e encaminhar mensagens irrecuperáveis para DLQ com processo de análise e replay.
- Não reprocessar DLQ cegamente.
- Em Kafka, escolher partition key preservando ordenação necessária e distribuição.
- Em RabbitMQ, configurar acknowledgements, prefetch, durabilidade, TTL e dead-lettering conscientemente.
