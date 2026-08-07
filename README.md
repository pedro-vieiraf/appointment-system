# Appointment System

Sistema de gerenciamento de agendamentos desenvolvido com Spring Boot.

O objetivo do projeto é construir uma API REST seguindo práticas utilizadas em ambientes profissionais, incluindo testes automatizados, CI/CD, Docker e boas práticas de arquitetura.

## Tecnologias

- Java 21
- Spring Boot
- Spring Data JPA
- PostgreSQL
- Docker
- Maven
- GitHub Actions

## Arquitetura

O projeto segue uma arquitetura em camadas:

```
Controller
    ↓
Service
    ↓
Repository
    ↓
Database
```

## Executando localmente

### Pré-requisitos

Antes de executar o projeto, certifique-se de possuir:

- Java 21
- Docker
- Docker Compose

### Subindo o banco de dados

```bash
docker compose up -d --build
```

Caso seja da sua preferência, a aplicação também pode ser executada pelo container Docker.

### Executando a aplicação

```bash
./mvnw spring-boot:run
```

A API estará disponível em:

```
http://localhost:8080
```

## Testes

Para executar os testes automatizados:

```bash
./mvnw test
```

## CI/CD

O projeto utiliza GitHub Actions para:

- Executar testes automaticamente;
- Validar o build da aplicação;
- Garantir que Pull Requests estejam funcionando corretamente antes do merge.

## Próximos passos

- [ ] Implementar gerenciamento de usuários;
- [ ] Implementar autenticação e autorização;
- [ ] Criar gerenciamento de agendamentos;
- [ ] Adicionar documentação OpenAPI;
- [ ] Adicionar cobertura de testes com JaCoCo.