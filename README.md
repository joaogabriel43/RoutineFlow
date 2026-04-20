# RoutineFlow

> Sistema de gerenciamento de rotina pessoal orientado a dados.
> Importe sua rotina via YAML, acompanhe seu progresso diário e visualize analytics detalhados.

![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React-18-61DAFB?style=flat-square&logo=react&logoColor=black)
![TypeScript](https://img.shields.io/badge/TypeScript-5.x-3178C6?style=flat-square&logo=typescript&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat-square&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker&logoColor=white)

---

## ✨ Features

- 📥 **Importação via YAML** — configure sua rotina em um arquivo estruturado, sem código
- ✅ **Check-in diário** — marque tarefas com atualização otimista instantânea (zero delay)
- 📅 **Visão semanal** — grid 7×N com destaque no dia atual e progresso por área
- 🔥 **Streak engine** — rastreamento de sequências de dias ativos por área
- 🗓️ **Heatmap de atividade** — 91 dias de histórico em grid CSS, estilo GitHub
- 📈 **Gráfico de progresso** — evolução semanal das últimas 8 semanas via Recharts
- ♻️ **Reset automático** — checkboxes resetam à meia-noite via Spring Scheduler
- 🔒 **Auth JWT** — autenticação stateless com Spring Security 6

---

## 🏗️ Arquitetura

```
┌─────────────────────────────────────────────────────────────────┐
│                         Frontend (React 18)                     │
│  LoginPage  TodayPage  WeekPage  AnalyticsPage  ImportPage     │
│          useToday  useWeek  useAnalytics  useImportRoutine      │
│                    TanStack Query + Axios                        │
└──────────────────────────────┬──────────────────────────────────┘
                               │ HTTP / REST
┌──────────────────────────────▼──────────────────────────────────┐
│                    Backend (Spring Boot 3.3)                     │
│                                                                  │
│  Presentation  →  Application  →  Domain  ←  Infrastructure     │
│  (Controllers)    (Use Cases)   (Entities)   (JPA / Security)   │
│                                                                  │
│  RoutineImportEngine (Strategy: YAML + TXT)                     │
│  CheckInEngine (upsert + optimistic) │ StreakCalculationService  │
│  Analytics (Streak, Heatmap, Weekly) │ Spring @Scheduled reset  │
└──────────────────────────────┬──────────────────────────────────┘
                               │ JDBC (Flyway migrations)
                    ┌──────────▼──────────┐
                    │    PostgreSQL 16     │
                    └─────────────────────┘
```

### Stack por camada

| Camada        | Tecnologias                                        |
|---------------|----------------------------------------------------|
| Frontend      | React 18, Vite, TypeScript, Tailwind CSS, shadcn/ui |
| API           | Java 17, Spring Boot 3.3, Spring Security 6, JJWT  |
| Persistência  | PostgreSQL 16, Spring Data JPA, Flyway             |
| Infra         | Docker, Docker Compose, nginx (SPA)                |

---

## 🚀 Setup em 3 comandos

```bash
git clone https://github.com/seu-usuario/routineflow.git
cd routineflow

cp .env.example .env
# Edite .env com um JWT_SECRET seguro (mín. 256 bits)

docker compose up --build
```

- **Frontend**: http://localhost
- **API**: http://localhost:8080/api
- **Flyway** roda as migrations automaticamente no boot da API

---

## 📐 Design Decisions

| Decisão | Justificativa |
|---------|---------------|
| **Clean Architecture** | Domínio isolado e testável sem Spring — domain models sem anotações JPA |
| **Strategy Pattern (Import Engine)** | Suporta YAML e TXT sem if/else no controller — extensível para novos formatos |
| **TDD** | 55+ testes escritos antes da implementação — use cases com Mockito + controllers com Testcontainers |
| **Optimistic Update** | UX sem delay no check-in: estado local atualiza imediatamente, API sincroniza em background |
| **DailyLog imutável** | Histórico nunca deletado — fonte de verdade para streak, heatmap e analytics |
| **Reset por query de data** | Checkboxes "resetam" por comparação de data no load, não por DELETE — preserva histórico |
| **React Query (useQueries)** | Queries paralelas no WeekPage (8 simultâneas) e AnalyticsPage sem waterfall |

---

## 📊 Cobertura de Testes

| Sprint | Escopo | Testes |
|--------|--------|--------|
| 1 — Auth | JwtService, registro, login, endpoints protegidos | ~12 |
| 2 — Import | YamlParser, TxtParser, ImportUseCase, RoutineController | ~15 |
| 3 — Check-in | CheckInUseCase, StreakEngine, reset scheduler | ~10 |
| 4 — Analytics | Streak, Heatmap, WeeklyCompletion, Comparison, History, AnalyticsController | ~20 |
| **Total** | | **55+** |

---

## 🗂️ Estrutura do Projeto

```
routineflow/
├── routineflow-api/                  # Spring Boot
│   └── src/main/java/com/routineflow/
│       ├── domain/                   # Entidades puras (sem JPA)
│       ├── application/              # Use cases + DTOs
│       ├── infrastructure/           # JPA entities, parsers, security
│       └── presentation/             # REST controllers
│
├── routineflow-web/                  # React + Vite
│   └── src/
│       ├── pages/                    # TodayPage, WeekPage, AnalyticsPage, ImportPage
│       ├── hooks/                    # useToday, useWeek, useAnalytics, useImportRoutine
│       ├── components/shared/        # AreaCard, TaskItem, EmptyRoutineState, NavBar
│       └── services/api.ts           # Axios + interceptors JWT
│
├── docker-compose.yml                # postgres + api + web
├── .env.example                      # Variáveis de ambiente (JWT_SECRET)
└── CLAUDE.md                         # Decisões de arquitetura + lições aprendidas
```

---

## 🔑 Variáveis de Ambiente

| Variável | Descrição | Padrão |
|----------|-----------|--------|
| `JWT_SECRET` | Chave secreta JWT (min. 256 bits) | *obrigatório em produção* |
| `DB_URL` | URL JDBC do PostgreSQL | `jdbc:postgresql://postgres:5432/routineflow` |
| `DB_USERNAME` | Usuário PostgreSQL | `postgres` |
| `DB_PASSWORD` | Senha PostgreSQL | `postgres` |

---

## 📄 Formato de Importação (YAML)

```yaml
routine:
  name: "Minha Rotina 2026"
  areas:
    - name: "Inglês / PTE"
      color: "#3B82F6"
      icon: "📚"
      schedule:
        MONDAY:
          - title: "Re-tell Lecture"
            description: "3 re-tells no PTE Wizard"
            estimatedMinutes: 30
        TUESDAY:
          - title: "Reading & Writing"
            estimatedMinutes: 45
```

Dias suportados: `MONDAY` `TUESDAY` `WEDNESDAY` `THURSDAY` `FRIDAY` `SATURDAY` `SUNDAY`
