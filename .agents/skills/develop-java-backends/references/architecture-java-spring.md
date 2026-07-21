# Arquitetura, Java e Spring Boot

## Sumário

1. Seleção arquitetural
2. Organização de módulos
3. Persistência e transações
4. Desempenho e concorrência
5. Configuração e dependências

## 1. Seleção arquitetural

Escolher arquitetura com base no domínio, tamanho da equipe, frequência de mudança, volume, disponibilidade e custo operacional.

| Contexto | Ponto de partida |
| --- | --- |
| CRUD simples | Camadas: API, serviço, repositório |
| Domínio rico | Aplicação + domínio isolado + adapters |
| Muitos módulos relacionados | Monólito modular com fronteiras verificáveis |
| Integrações substituíveis | Portas e adapters |
| Serviços independentes | Microsserviços somente com ownership e operação maduros |

Definir dependências apontando para dentro quando usar arquitetura limpa. Evitar fazer o domínio depender de Spring, HTTP, ORM ou mensageria sem necessidade.

## 2. Organização de módulos

- Organizar preferencialmente por capacidade de negócio, não apenas por tipo técnico.
- Tornar fronteiras explícitas e impedir acesso acidental entre módulos.
- Manter contratos externos separados dos modelos de persistência.
- Usar mapeamento explícito quando ele melhora controle e evolução; evitar abstrações de mapeamento opacas em domínios sensíveis.
- Registrar decisões relevantes em ADR curto quando houver trade-off duradouro.

## 3. Persistência e transações

- Delimitar `@Transactional` na camada de aplicação e usar `readOnly = true` para leituras quando útil.
- Não depender de Open Session in View para corrigir carregamento preguiçoso; buscar os dados necessários de forma deliberada.
- Detectar N+1 com testes, logs ou métricas; usar fetch join, entity graph ou projeções conforme o caso.
- Evitar cascade e orphan removal amplos sem compreender o ciclo de vida do agregado.
- Implementar paginação determinística; considerar keyset pagination em grandes volumes.
- Usar optimistic locking em concorrência comum e pessimistic locking apenas quando comprovadamente necessário.
- Fazer migrações de esquema por expansão e contração quando houver versões simultâneas.
- Não usar `ddl-auto` destrutivo fora de ambientes efêmeros controlados.

## 4. Desempenho e concorrência

- Medir antes de otimizar: latência p50/p95/p99, throughput, erros, CPU, memória, GC, pool de conexões e queries.
- Dimensionar pools considerando banco, instâncias e concorrência; não aumentar limites isoladamente.
- Usar cache apenas com estratégia explícita de chave, TTL, invalidação, consistência e prevenção de stampede.
- Tratar virtual threads, programação reativa e assíncrona como modelos diferentes; escolher conforme carga e bibliotecas.
- Evitar estado mutável compartilhado em singletons Spring.
- Definir limites de fila, executores nomeados e política de rejeição para tarefas assíncronas.

## 5. Configuração e dependências

- Fixar versão de Java e toolchain; respeitar a matriz de compatibilidade do Spring Boot.
- Preferir BOM e dependency management da plataforma.
- Externalizar configuração e validar propriedades obrigatórias no startup.
- Usar perfis para diferenças reais de ambiente, não para duplicar toda a configuração.
- Manter dependências mínimas e remover bibliotecas não usadas.
- Atualizar dependências críticas com notas de versão, testes e plano de reversão.
