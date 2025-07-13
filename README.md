```markdown
# Rinha de Backend 2025 - Calixtoneto

Esta é a minha submissão para a Rinha de Backend 2025. O backend foi desenvolvido com foco em alta performance, baixa latência e consistência para maximizar o lucro, minimizando taxas e penalidades.

## Tecnologias Utilizadas

- **Linguagem**: Java 21
- **Framework**: Spring Boot 3.3.2 com WebFlux (programação reativa)
- **Banco de Dados**: MongoDB 7 (com Spring Data MongoDB Reactive)
- **Cache**: Redis 7 (para cache de health-checks)
- **Balanceamento de Carga**: NGINX
- **Resiliência**: Resilience4j (Circuit Breaker e Retry)
- **Mapeamento de Objetos**: ModelMapper
- **Geração de API**: OpenAPI Generator (baseado em `openapi.yaml`)
- **Build**: Maven
- **Testes de Carga**: k6

## Características

- **Endpoints**: Implementa `POST /payments` para criar pagamentos e `GET /payments-summary` para obter resumo de pagamentos, conforme especificações.
- **Resiliência**: Usa Circuit Breaker e Retry para lidar com instabilidades dos processadores de pagamento.
- **Performance**: Otimizado para baixa latência com índices no MongoDB (`requestedAt` e `correlationId`), processamento em streaming e cache de health-checks no Redis.
- **Consistência**: Garante consistência salvando pagamentos no MongoDB antes de enviar ao processador e validando `correlationId` único.
- **Configuração**: URLs dos processadores de pagamento e conexão com MongoDB/Redis configuradas no `application.yml`.

## Repositório

O código-fonte está disponível em: [https://github.com/calixtoneto/rinha-backend-2025](https://github.com/calixtoneto/rinha-backend-2025)

## Pré-requisitos

- **Java 21**: Instalado para build local.
- **Maven**: Para compilar o projeto.
- **Docker**: Para rodar os containers (MongoDB, Redis, NGINX, e backend).
- **k6**: Para testes de carga.
- **Payment Processors**: Containers dos processadores de pagamento disponíveis (veja `payment-processor/docker-compose.yml`).

## Instruções para Executar

1. **Clone o Repositório**:
   ```bash
   git clone https://github.com/calixtoneto/rinha-backend-2025.git
   cd rinha-de-backend-2025
   ```

2. **Suba os Payment Processors**:
   - Navegue até o diretório dos processadores de pagamento (fornecido pela Rinha):
     ```bash
     cd payment-processor
     docker-compose up -d
     ```

3. **Construa e Inicie o Backend**:
   - Execute o comando único para compilar, construir a imagem Docker e iniciar os containers:
     ```bash
     mvn clean generate-sources install -DskipTests && docker build -t calixtoneto/rinha-backend-2025:latest . && docker-compose up -d
     ```

4. **Verifique os Containers**:
   - Confirme que os serviços `backend1`, `backend2`, `mongodb`, `redis`, e `nginx` estão rodando:
     ```bash
     docker-compose ps
     ```

5. **Teste os Endpoints Manualmente**:
   - Crie um pagamento:
     ```bash
     curl -X POST http://localhost:9999/payments -H "Content-Type: application/json" -d '{"correlationId": "550e8400-e29b-41d4-a716-446655440000", "amount": 19.90}'
     ```
   - Obtenha o resumo de pagamentos:
     ```bash
     curl "http://localhost:9999/payments-summary?from=2025-07-13T02:00:01.897Z&to=2025-07-13T02:00:11.797Z"
     ```

6. **Execute Testes de Carga com k6**:
   - Baixe o script de teste da Rinha:
     ```bash
     mkdir rinha-test
     curl -o rinha-test/rinha.js https://raw.githubusercontent.com/zanfranceschi/rinha-de-backend-2025/main/rinha-test/rinha.js
     curl -o rinha-test/requests.js https://raw.githubusercontent.com/zanfranceschi/rinha-de-backend-2025/main/rinha-test/requests.js
     ```
   - Execute o teste:
     ```bash
     cd rinha-test
     k6 run rinha.js
     ```

## Docker

A imagem do backend está publicada no Docker Hub: `calixtoneto/rinha-backend-2025:latest`.

## Solução de Problemas

- **Erro `HTTP 404` no `/payments-summary`**:
  - Verifique os logs do backend:
    ```bash
    docker-compose logs backend1
    docker-compose logs backend2
    ```
  - Confirme que o `openapi.yaml` define corretamente o endpoint `/payments-summary`.
  - Teste diretamente nos backends:
    ```bash
    docker exec -it <container_id_backend1> curl http://localhost:9999/payments-summary?from=2025-07-13T02:00:01.897Z&to=2025-07-13T02:00:11.797Z
    ```

- **Erro no NGINX**:
  - Verifique o arquivo `nginx.conf` e os logs:
    ```bash
    docker-compose logs nginx
    ```

- **Erro no k6**:
  - Se o teste falhar com `TypeError` ou `HTTP 404`, aplique as correções sugeridas no `rinha.js` e verifique os logs do backend.

## Contato

Para dúvidas ou sugestões, abra uma issue no repositório ou contate-me via [GitHub](https://github.com/calixtoneto).
```