# Segurança e DevSecOps

## Sumário

1. Threat modeling
2. Autenticação e sessão
3. Autorização
4. Proteções OWASP
5. Segredos e cadeia de suprimentos
6. Revisão de segurança

## 1. Threat modeling

Para mudanças sensíveis, identificar ativos, atores, limites de confiança, entradas, saídas, privilégios, ameaças e controles. Priorizar riscos por probabilidade e impacto. Considerar abuso legítimo da funcionalidade, não apenas ataques técnicos.

## 2. Autenticação e sessão

- Preferir provedor de identidade e protocolos padronizados.
- Validar JWT com algoritmo permitido explicitamente; rejeitar token expirado, emissor ou audiência incorretos e assinatura inválida.
- Usar access tokens curtos e rotação segura de refresh tokens quando aplicável.
- Detectar reutilização de refresh token quando o modelo exigir.
- Invalidar ou revogar sessões sensíveis conforme risco e capacidade do sistema.
- Aplicar rate limit e defesa contra credential stuffing; não revelar se conta existe.
- Armazenar senha com Argon2id, bcrypt ou mecanismo atual aceito pelo framework, configurado conforme capacidade do ambiente.
- Exigir MFA para operações e perfis de alto risco quando o produto suportar.

## 3. Autorização

- Aplicar least privilege e deny-by-default.
- Validar acesso ao objeto específico para evitar IDOR/BOLA.
- Separar roles amplas de permissions quando o domínio exigir granularidade.
- Aplicar method security ou política equivalente perto da regra protegida.
- Não confiar em role, tenant, owner ou preço recebidos do cliente.
- Em multi-tenant, filtrar tenant em todas as queries e considerar isolamento adicional no banco.
- Auditar mudanças administrativas e operações sensíveis sem registrar segredos.

## 4. Proteções OWASP

- SQL/NoSQL injection: parametrizar queries e restringir expressões dinâmicas.
- XSS: retornar conteúdo com tipo correto, sanear HTML permitido e delegar encoding contextual ao frontend.
- CSRF: habilitar proteção para autenticação baseada em cookies; documentar exceções apenas para APIs stateless reais.
- SSRF: usar allowlist de destinos, resolver/redirecionar com cuidado, bloquear redes internas e limitar protocolo, tamanho e tempo.
- Deserialização: aceitar DTOs explícitos, restringir polimorfismo e ignorar propriedades privilegiadas do cliente.
- Upload: validar extensão, MIME e assinatura; limitar tamanho; renomear; armazenar fora da área executável; analisar quando necessário.
- Path traversal: não concatenar caminhos fornecidos pelo usuário; normalizar e restringir ao diretório permitido.
- Mass assignment: mapear apenas campos permitidos.
- DoS: impor limites de payload, paginação, queries, conexão, timeout e taxa.
- Headers: configurar HSTS, CSP e demais headers na camada apropriada, sem duplicidade conflitante.

## 5. Segredos e cadeia de suprimentos

- Usar secret manager ou mecanismo seguro da plataforma; nunca incluir segredo em Git, imagem, frontend ou logs.
- Fornecer arquivos `.env.example` somente com nomes e valores fictícios.
- Rotacionar credencial exposta; removê-la do código não basta.
- Fixar imagens e actions confiáveis; preferir digest em produção sensível.
- Gerar SBOM quando exigido e analisar dependências com ferramenta compatível.
- Verificar advisories em fontes oficiais e avaliar explorabilidade, não apenas severidade nominal.
- Aplicar patches com testes e plano de reversão.

## 6. Revisão de segurança

Verificar antes da entrega:

- autenticação e expiração;
- autorização horizontal e vertical;
- validação e normalização de entrada;
- queries parametrizadas;
- CORS e CSRF conforme o modelo de credenciais;
- ausência de segredos e dados sensíveis em resposta ou log;
- rate limits e timeouts em superfícies expostas;
- dependências e imagens vulneráveis;
- mensagens de erro sem stack trace ou detalhes internos;
- auditoria para ações críticas;
- testes negativos dos controles principais.
