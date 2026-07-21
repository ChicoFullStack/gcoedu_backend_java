# DevOps e deploy autorizado

## Sumário

1. Build e artefatos
2. Containers e proxy
3. CI/CD
4. Migrações e estratégias de entrega
5. Checklist de autorização e execução

## 1. Build e artefatos

- Produzir build reproduzível com versão identificável e dependências resolvidas de fonte confiável.
- Separar compile/test/package e publicar o mesmo artefato promovido entre ambientes.
- Não embutir configuração específica de produção no artefato.
- Gerar checksum, provenance ou assinatura quando exigido pelo risco.

## 2. Containers e proxy

- Usar multi-stage build, runtime mínimo, usuário não root e filesystem read-only quando possível.
- Fixar imagem por versão específica; analisar vulnerabilidades.
- Definir limites de CPU/memória, sinal de shutdown, graceful termination e health checks.
- Não copiar `.git`, arquivos locais, credenciais ou caches desnecessários para a imagem.
- Em Nginx ou proxy equivalente, configurar TLS, limites, timeouts, headers e encaminhamento de IP/protocolo de modo coerente.
- Não expor banco, Redis, broker ou Actuator publicamente sem necessidade e controles fortes.

## 3. CI/CD

Pipeline recomendado, ajustado ao projeto:

1. checkout confiável e toolchain fixada;
2. validação de estilo e análise estática;
3. compilação e testes unitários;
4. testes de integração/contrato;
5. análise de dependências, segredos e container;
6. build e identificação do artefato;
7. publicação em registry autorizado;
8. promoção com gates por ambiente;
9. migração controlada;
10. deploy, smoke tests e observação;
11. rollback ou roll-forward quando necessário.

Proteger branches, ambientes e credenciais. Conceder ao pipeline apenas permissões necessárias e preferir credenciais efêmeras.

## 4. Migrações e estratégias de entrega

- Fazer backup e testar restauração antes de mudança de alto risco.
- Usar expand/contract para colunas e contratos compartilhados.
- Evitar misturar mudança incompatível de schema com aplicação que depende imediatamente dela.
- Escolher rolling, blue-green ou canary conforme capacidade da plataforma e compatibilidade da aplicação.
- Garantir backward compatibility durante versões sobrepostas.
- Definir rollback; para migração de dados, muitas vezes planejar roll-forward seguro é mais realista.

## 5. Checklist de autorização e execução

Não iniciar ação externa sem autorização explícita. Antes de agir, confirmar:

- ambiente e conta/projeto corretos;
- serviço, região, domínio e versão alvo;
- imagem ou artefato exato;
- variáveis e segredos existentes sem exibi-los;
- impacto, janela e responsáveis;
- backup/restauração e plano de rollback;
- migrations pendentes e compatibilidade;
- health checks, smoke tests e sinais de sucesso;
- forma de interromper com segurança.

Depois de um deploy autorizado:

1. verificar rollout e instâncias;
2. verificar readiness, logs, erros, latência e saturação;
3. executar smoke tests não destrutivos;
4. confirmar migrações e compatibilidade;
5. observar pelo período proporcional ao risco;
6. executar rollback/roll-forward se os critérios falharem;
7. registrar versão, resultado e pendências sem revelar segredos.
