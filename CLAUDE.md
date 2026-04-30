# CLAUDE.md — RoutineFlow
> Versão: 2.7.0 | Criado: 2026-04-19 | Última atualização: 2026-04-30

---

## 🏗️ Visão Geral do Projeto

**RoutineFlow** é um sistema web de gerenciamento de rotina pessoal orientado a dados. O diferencial principal é a **importação de rotinas via arquivo estruturado** (YAML/TXT), eliminando dados hardcoded e permitindo que qualquer usuário configure sua própria rotina sem tocar no código.

**Objetivo de portfólio**: Demonstrar domínio em Java backend (Clean Architecture, DDD, TDD, Strategy Pattern, Scheduled Jobs, Analytics via queries otimizadas) com frontend React moderno como vitrine.

**Potencial de produto**: Arquitetura modular que permite evolução para SaaS no futuro.

---

## 🛠️ Stack Técnica

### Backend
- **Java 17** — records, sealed classes, text blocks
- **Spring Boot 3.3** — Spring Web, Spring Security 6, Spring Data JPA, Spring Scheduler
- **PostgreSQL 16** — banco principal
- **Flyway** — migrations versionadas
- **Testcontainers** — testes de integração com PostgreSQL real
- **JUnit 5 + Mockito** — testes unitários
- **Maven** — build tool

### Frontend
- **React 18 + Vite + TypeScript**
- **Tailwind CSS** — estilização utility-first
- **shadcn/ui** — componentes prontos, design clean estilo Apple
- **Recharts** — gráficos de analytics
- **React Query** — gerenciamento de estado servidor

### Auth
- **JWT** — stateless authentication
- **Spring Security 6** — filtros e configuração

### DevOps
- **Docker + Docker Compose** — ambiente local e deploy
- **Railway ou Render** — deploy da aplicação

---

## 📁 Estrutura de Pastas

### Backend
```
routineflow-api/
├── src/
│   ├── main/
│   │   ├── java/com/routineflow/
│   │   │   ├── domain/
│   │   │   │   ├── model/          # Entidades de domínio puras (sem anotações JPA)
│   │   │   │   ├── repository/     # Interfaces (portas de saída)
│   │   │   │   └── service/        # Regras de negócio puras
│   │   │   ├── application/
│   │   │   │   ├── usecase/        # Casos de uso (orquestração)
│   │   │   │   └── dto/            # Records de request/response
│   │   │   ├── infrastructure/
│   │   │   │   ├── persistence/    # Entities JPA + Repository impl
│   │   │   │   ├── parser/         # Strategy Pattern para importação de arquivos
│   │   │   │   └── security/       # JWT filter, UserDetailsService, config
│   │   │   └── presentation/
│   │   │       └── controller/     # REST Controllers
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/       # V1__*, V2__* — Flyway
│   └── test/
│       └── java/com/routineflow/
│           ├── unit/               # Testes unitários por camada
│           └── integration/        # @SpringBootTest + Testcontainers
└── pom.xml

```

### Frontend
```
routineflow-web/
├── src/
│   ├── components/
│   │   ├── ui/         # shadcn/ui components
│   │   └── shared/     # componentes reutilizáveis do projeto
│   ├── pages/          # Today, Week, Analytics, Settings
│   ├── hooks/          # custom hooks (useToday, useStreak, etc.)
│   ├── services/       # chamadas à API (axios)
│   ├── types/          # TypeScript interfaces
│   └── lib/            # utils, constants
└── vite.config.ts
```

---

## ⚙️ Configurações do Ambiente

### Variáveis de ambiente necessárias (.env)
```
# Backend
DB_URL=jdbc:postgresql://localhost:5432/routineflow
DB_USERNAME=postgres
DB_PASSWORD=postgres
JWT_SECRET=your-secret-key-min-256-bits
JWT_EXPIRATION=86400000

# Frontend
VITE_API_URL=http://localhost:8080/api
```

### Docker Compose (desenvolvimento local)
```yaml
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: routineflow
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
```

### Testcontainers
- Sempre usar `@Testcontainers` + `PostgreSQLContainer` nos testes de repositório
- Nunca mockar banco nos testes de integração — usar container real

### PWA — vite-plugin-pwa
- Configurado com `registerType: 'autoUpdate'`
- API sempre `NetworkOnly` — nunca cachear `/api/*` (padrão `/\/api\//i`)
- `sw.js` e `manifest.webmanifest` gerados automaticamente no `npm run build`
- Ícones em `public/icons/` gerados via `scripts/generate-icons.js` (sharp)
- `vercel.json`: header `Cache-Control: no-cache` para `sw.js` + `Content-Type: application/manifest+json` para o manifest
- `InstallPrompt`: só renderiza no mobile (`window.matchMedia('(max-width: 768px)')`), dispensável via localStorage (`rf_install_dismissed`)
- `react-is` deve estar instalado como dependência — recharts depende dele e o rolldown não resolve transitivo em builds mais novos

---

## 📐 Padrões do Projeto

### 1. DTOs como Records Java 17
```java
// SEMPRE assim — imutável, sem boilerplate
public record CreateTaskRequest(
    @NotBlank String title,
    @NotBlank String description,
    @NotNull DayOfWeek dayOfWeek
) {}
```

### 2. Constructor Injection obrigatório
```java
// CERTO
@Service
public class CheckInService {
    private final CheckInRepository checkInRepository;
    
    public CheckInService(CheckInRepository checkInRepository) {
        this.checkInRepository = checkInRepository;
    }
}

// ERRADO — nunca usar
@Autowired
private CheckInRepository checkInRepository;
```

### 3. Nunca expor entidades JPA nos controllers
- Controller recebe DTO → chama UseCase → UseCase usa domínio → retorna DTO de resposta
- Entidade JPA nunca sai da camada de infrastructure/persistence

### 4. ResponseEntity explícito em todos os endpoints
```java
return ResponseEntity.status(HttpStatus.CREATED).body(response);
// nunca return response; diretamente
```

### 5. TDD — testes ANTES da implementação
- Escrever o teste que falha → implementar para fazer passar → refatorar
- Todo use case novo: mínimo 1 teste unitário + 1 teste de integração
- Nomenclatura de testes: `methodName_scenario_expectedResult`

### 6. Strategy Pattern para o Routine Import Engine
```java
public interface RoutineFileParser {
    boolean supports(String fileExtension);
    ParsedRoutine parse(InputStream inputStream);
}

// Implementações: YamlRoutineParser, TxtRoutineParser
```

### 8. Streak: área sem tarefas agendadas no dia → não altera streak
`StreakCalculationService` verifica `getDayOfWeek()` antes de qualquer operação.
Dia sem tarefas não conta como "quebra" — streak permanece intacto.
Implementado em: `StreakCalculationService.calculate()` com `if (!hasTasksToday) continue`.

### 7. Heatmap: range sempre começa numa segunda-feira
`buildHeatmapRange()` computa `from` como a segunda-feira de 12 semanas atrás em relação à segunda-feira atual.
Isso garante que o CSS grid de 7 colunas nunca precise de células de offset — sempre começa na coluna correta.
Dias futuros são marcados com `isFuture: true` e renderizados com cor `#111111` (inativa), nunca com cor de atividade.

### 8. WeekPage: coluna esquerda sticky com z-10
Para scroll horizontal no mobile não sobrepor os nomes das áreas, a coluna esquerda usa `sticky left-0`.
`bg-[#141414]` é obrigatório na coluna sticky — sem background ela fica transparente e o grid passa por baixo.
`z-10` garante que a coluna fique acima dos círculos de dia durante o scroll.

### 10. deleteTask nunca toca em DailyLog — cascade do banco cuida disso
`TaskUseCase.deleteTask()` chama apenas `taskJpaRepository.deleteById()`.
O `ON DELETE CASCADE` da FK `daily_logs.task_id → tasks.id` (definido no Flyway) lida com a limpeza dos registros filhos.
`DailyLog` é trilha de auditoria imutável — NUNCA deletar explicitamente via código Java.
Qualquer chamada a `dailyLogRepository.deleteBy*` neste contexto é um bug grave.

### 11. PATCH /reorder — IDs completos da lista, não deltas
`reorderAreas` e `reorderTasks` recebem a lista **completa** de IDs na nova ordem (não apenas os que mudaram).
O use case re-atribui `orderIndex = posição_na_lista` para cada item.
Frontend deve sempre enviar todos os IDs da área/tarefa, não apenas o que moveu.

### 9. Frontend: completed status por área, não por task individual
O backend retorna `completedTasks: number` por área em `DailyProgressResponse` — não IDs individuais de tasks.
**Solução adotada**: No hook `useToday`, ao mesclar schedule + progress, as primeiras N tasks (ordenadas por `orderIndex`) são marcadas como concluídas onde N = `completedTasks` da área.
```typescript
tasks: sorted.map((task, idx) => ({
  ...task,
  completed: idx < completed,  // N primeiras = concluídas
  completedAt: idx < completed ? new Date().toISOString() : null,
}))
```
**Trade-off aceito**: Ordem de conclusão não é preservada entre sessões (sempre as primeiras N tasks ficam marcadas). Aceitável para portfolio demo.
**Evolução futura**: Se backend evoluir para retornar `completedTaskIds: number[]`, trocar `idx < completed` por `completedIds.has(task.id)`.

### 12. Export CSV: BOM UTF-8 + StreamingResponseBody
`ExportController` sempre escreve 3 bytes de BOM UTF-8 (0xEF 0xBB 0xBF) antes do header do CSV.
Sem o BOM, o Excel no Windows interpreta o arquivo como ANSI e quebra acentos.
`StreamingResponseBody` escreve linha a linha — nunca carrega todo o histórico em memória.
Máximo de 365 dias por request (lança `IllegalArgumentException` se exceder).
Default: últimos 90 dias. Query em `DailyLogJpaRepository.findForExport` com JOIN tasks → areas → routine para garantir ownership por `userId`.

### 13. HabitNow Parser: roda 100% no frontend
Formato `.hn`: `B[timestamp]{H[hábitos separados por |]{X[logs]...`
Epoch de datas HabitNow: 2012-01-01 + base-36.
Dias: 1=Dom, 2=Seg, 3=Ter, 4=Qua, 5=Qui, 6=Sex, 7=Sáb.
Campo de dias vazio = todo dia → expande para todos os 7 dias no YAML gerado.
Hábito ativo: dateRange com 1 data. Inativo: 2 datas (início + arquivamento).
Parser em `src/lib/habitnow-parser.ts` — zero chamadas de API, processamento local via FileReader.

### 8. Formato do arquivo de importação (YAML)
```yaml
routine:
  name: "Minha Rotina 2026"
  areas:
    - name: "Inglês/PTE"
      color: "#3B82F6"
      icon: "📚"
      schedule:
        MONDAY:
          - title: "Re-tell Lecture"
            description: "3 re-tells no PTE Wizard"
            estimatedMinutes: 30
```

---

## 🏛️ ADRs — Decisões de Arquitetura

### ADR-001: React + Vite ao invés de Angular
**Contexto**: João tem Angular no stack, mas o foco do projeto é demonstrar backend Java. Frontend é vitrine.
**Decisão**: React 18 + Vite + Tailwind + shadcn/ui para resultado visual clean e rápido com menos boilerplate.
**Consequências**: (+) Menos tokens de prompt no frontend, resultado visual melhor. (-) Não agrega Angular no CV.
**Status**: Aceita

### ADR-002: Clean Architecture com separação domínio puro
**Contexto**: Projeto com potencial de produto futuro — precisa de manutenibilidade.
**Decisão**: Domain model sem anotações JPA. Entidades JPA separadas na camada infrastructure.
**Consequências**: (+) Domínio testável sem Spring. (-) Mapeamento extra entre domain model e JPA entity.
**Status**: Aceita

### ADR-003: Importação via arquivo como feature central
**Contexto**: Rotinas hardcoded no banco são frágeis e não demonstram padrões de design.
**Decisão**: Routine Import Engine com Strategy Pattern (YAML e TXT). Banco recebe dados já parseados.
**Consequências**: (+) Demonstra Strategy Pattern, processamento de arquivo, validação. (+) Modular e extensível.
**Status**: Aceita

### ADR-004: Analytics via queries otimizadas, sem AI
**Contexto**: João quer demonstrar domínio de dados sem complexidade de LLM.
**Decisão**: Streak engine, heatmap, taxa de conclusão — tudo calculado via PostgreSQL com queries agregadas.
**Consequências**: (+) Demonstra otimização de queries e modelagem. (-) Sem diferencial de AI.
**Status**: Aceita

### ADR-005: Reset automático via Spring @Scheduled
**Contexto**: Checkboxes precisam resetar à meia-noite sem intervenção manual.
**Decisão**: Spring Scheduler com cron job. Histórico nunca é deletado — apenas o estado do dia muda.
**Consequências**: (+) Simples, sem dependência externa. Histório de DailyLog sempre preservado.
**Status**: Aceita

### ADR-007: MERGE não cria nova rotina — adiciona na ativa
**Contexto**: Usuários com rotina ativa querem enriquecer com novos hábitos sem perder histórico.
**Decisão**: `ImportMode.MERGE` localiza a rotina ativa e adiciona apenas áreas/tarefas ausentes. Sem `deactivateAllByUserId`, sem novo `RoutineJpaEntity`. Fallback para REPLACE quando não há rotina ativa.
**Consequências**: (+) Histórico de DailyLog preservado integralmente. (+) Idempotente (re-importar o mesmo arquivo é safe). (-) Nome da rotina não é atualizado no MERGE.
**Status**: Aceita

### ADR-006: Ownership check retorna 404, não 403
**Contexto**: Ao buscar área ou task por ID, verificar se pertence ao usuário autenticado.
**Decisão**: `findByIdAndUserId` / `findByIdAndArea_User_Id` — se não encontrar (seja por não existir ou por não ser do usuário), lança `ResourceNotFoundException` → 404.
**Consequências**: (+) Não vaza informação sobre existência do recurso. (-) Comportamento opaco para depuração. Padrão de segurança por obscuridade intencional.
**Status**: Aceita

---

## 🗺️ Roadmap de Sprints

| Sprint | Foco | Status |
|--------|------|--------|
| Sprint 1 | Domain model + Auth (JWT) + Setup | ✅ Concluído |
| Sprint 2 | Routine Import Engine (Strategy Pattern) | ✅ Concluído |
| Sprint 3 | Daily Check-in + Reset Automático | ✅ Concluído |
| Sprint 4 | Analytics API (Streak, Heatmap, Weekly) | ✅ Concluído |
| Sprint 5 | Frontend React (Setup + TodayPage + WeekPage + AnalyticsPage) | ✅ Concluído |
| Sprint 6 | ImportPage + Polish + Docker + README | ✅ Concluído |
| Sprint 7 | CRUD de Áreas e Tarefas (TDD) + ManagePage | ✅ Concluído |
| Sprint 8 | Reset Frequency por Área (DAILY/WEEKLY/MONTHLY) + TDD + Frontend badge/selector | ✅ Concluído |
| Sprint 9 | Analytics por Área Individual (AreaAnalyticsUseCase, bestStreak, AreaAnalyticsPage) | ✅ Concluído |
| Sprint 10 | Export CSV (StreamingResponseBody, BOM UTF-8) + Conversor HabitNow (parser frontend-only) | ✅ Concluído |
| Sprint 11 | PWA instalável (vite-plugin-pwa, manifest, service worker, ícones, InstallPrompt) | ✅ Concluído |
| Sprint 12 | Navegação temporal (DateNavBar, useDay, ?date= em check-in, data futura → 400) | ✅ Concluído |
| Sprint 13 | Single tasks one-time (V10 migration, SingleTaskUseCase, /today endpoint, SingleTasksPage, TodayPage seção "Para fazer") | ✅ Concluído |
| Sprint 14 | ScheduleType (DAY_OF_WEEK \| DAY_OF_MONTH) + dayOfMonth em tasks — V11 migration, TDD backend, frontend toggle | ✅ Concluído |
| Sprint 15 | Import merge mode (REPLACE \| MERGE) — ImportMode enum, ImportRoutineResponse extendido, MERGE logic, ?mode param, 10 unit + 4 integration tests, frontend modal de modo + toast detalhado | ✅ Concluído |

---

## 🐛 Erros Conhecidos e Como Evitá-los

### [2026-04-19] Testes de integração requerem Docker ativo
**O que acontece**: `AuthControllerTest` e outros testes com `@Testcontainers` falham com conexão recusada se o Docker Desktop não estiver rodando.
**Por que**: Testcontainers sobe um container PostgreSQL real em tempo de execução.
**Como prevenir**: Iniciar o Docker Desktop antes de `./mvnw test`.
**Rodar apenas unitários sem Docker**: `./mvnw test -Dtest="JwtServiceTest,YamlRoutineParserTest,TxtRoutineParserTest,ImportRoutineUseCaseTest"`

### [2026-04-20] Spring Security 6 — unauthenticated request retorna 403, não 401
**O que acontece**: Endpoints protegidos sem token retornam 403 em vez de 401. Testes que esperam 401 falham.
**Por que**: No Spring Security 6, usuários anônimos são "autenticados como ROLE_ANONYMOUS". Ao acessar endpoint protegido, dispara `AccessDeniedException` (→ 403) em vez de `AuthenticationException` (→ 401).
**Como prevenir**: Sempre configurar `exceptionHandling` explicitamente no `SecurityConfig`:
```java
.exceptionHandling(ex -> ex
    .authenticationEntryPoint((req, res, authEx) ->
        res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
    .accessDeniedHandler((req, res, accessEx) ->
        res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
)
```
**Arquivo**: `infrastructure/security/SecurityConfig.java`

### [2026-04-20] Testes de integração falham com "ApplicationContext failure threshold exceeded" quando Docker não está ativo
**O que acontece**: Todos os 48+ testes de integração falham com a mensagem de threshold, mascarando a causa raiz.
**Por que**: O primeiro teste tenta subir o contexto Spring (que usa Testcontainers para PostgreSQL), falha pois Docker não está rodando, e todos os demais já pulam com "threshold exceeded".
**Como diagnosticar**: Rodar `./mvnw test -Dtest="AuthControllerTest#nomeDoTeste" -e` — a causa raiz aparece no log de startup do Tomcat.
**Como prevenir**: Iniciar o Docker Desktop antes de qualquer `./mvnw test`. Verificar com `docker ps`.

### [2026-04-19] TxtRoutineParser — "malformado" significa título vazio, não ausência de pipe
**O que acontece**: Linha `MONDAY: semPipe` (sem `|`) é válida — "semPipe" vira o título da task. O teste de "malformado" deve usar `MONDAY: ` (espaço vazio após `:`), que aciona a guarda `parts[0].isBlank()`.
**Por que**: O parser aceita tasks sem metadados opcionais (desc, min). Só o título é obrigatório.
**Como prevenir**: Fixture de "malformado" deve ter título vazio, não linha sem pipe.

### [2026-04-23] Heatmap cortava semana parcial mais recente
**O que aconteceu**: `Math.floor(days.length / 7)` cortava silenciosamente a última semana quando o range não era múltiplo de 7.
**Por que**: Floor arredonda para baixo — semana incompleta era ignorada. Com 88 dias (segunda Jan 26 → quinta Abr 23), `Math.floor(88/7) = 12` produzia apenas 84 células, excluindo os 4 dias mais recentes.
**Como prevenir**: Sempre usar `Math.ceil` para calcular número de semanas em grids de heatmap. Dias ausentes (fim de semana parcial) devem ser tratados como `undefined` e filtrados com `if (item) transposed.push(item)`.

### [2026-04-23] DAYOFWEEK() não existe no PostgreSQL
**O que aconteceu**: Tentativa de usar `DAYOFWEEK(dl.logDate)` em `@Query` nativeQuery para agrupar logs por dia da semana — funciona no MySQL mas falha no PostgreSQL com "function dayofweek does not exist".
**Por que**: `DAYOFWEEK()` é função proprietária do MySQL. PostgreSQL usa `EXTRACT(DOW FROM ...)` (0=domingo) ou `DATE_PART('dow', ...)`.
**Como prevenir**: Para agrupamentos por dia da semana, buscar as datas brutas (`List<LocalDate>`) e agrupar via `Collectors.groupingBy(LocalDate::getDayOfWeek)` em Java — sem dependência de função SQL proprietária, totalmente testável em unit tests sem banco.
**Arquivo**: `DailyLogJpaRepository.findCompletedLogDatesByAreaId` + `AreaAnalyticsUseCase.buildDayOfWeekStats()`

### [2026-04-30] UnnecessaryStubbingException em testes de validação com Mockito
**O que aconteceu**: Testes que esperam `IllegalArgumentException` em `TaskUseCase.createTask()` stubavam `taskJpaRepository.findByAreaIdOrderByOrderIndex()`, mas `validateSchedule()` é chamado ANTES dessa linha — o stub nunca é invocado.
**Por que**: Mockito's `@ExtendWith(MockitoExtension.class)` habilita strict stubbing por padrão — stubs não utilizados são erro.
**Como prevenir**: Em testes que esperam exceção lançada ANTES de uma chamada de repositório, não stubar esse repositório. Inspecionar o fluxo de chamadas no use case antes de adicionar stubs.
**Regra**: Apenas stubar o que realmente será chamado no caminho de código que o teste exercita.

### [2026-04-23] CORS bloqueando /auth/** em produção — 403 no login
**O que aconteceu**: `POST /api/auth/login` retornava 403 em produção (Railway + Vercel).
**Por que**: Duas causas combinadas:
1. `allowedOriginPatterns` não incluía o domínio exato da Vercel — preflight OPTIONS era rejeitado pelo CORS filter antes mesmo do Spring Security
2. `JwtAuthenticationFilter` não tinha `shouldNotFilter()` — podia interceptar `/auth/**` em edge cases
**Como prevenir**:
- Sempre adicionar o domínio de produção **explicitamente** no CORS (não depender só de `FRONTEND_URL` env var)
- Sempre implementar `shouldNotFilter()` no `JwtAuthenticationFilter` para pular `/auth/**` e `/actuator/**`
- Usar `/auth/**` (wildcard) em `requestMatchers` em vez de paths exatos
```java
// JwtAuthenticationFilter.java
@Override
protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getServletPath(); // retorna path SEM context-path /api
    return path.startsWith("/auth/") || path.startsWith("/actuator/");
}
```

---

## 🚀 Otimizações e Performance

### Streak Engine — regra crítica
- Streak é calculado olhando para `daily_logs` em ordem decrescente de data
- Query deve usar índice em `(user_id, area_id, log_date)` — criar este índice no Flyway
- Nunca calcular streak em memória iterando todos os logs — sempre via query SQL com `LAG()`

### Analytics por área — queries sem N+1
- `countCompletedByAreaId` e `findCompletedLogDatesByAreaId` usam `@Query` JPQL com JOIN direto — uma query cada
- Todas as datas de conclusão são buscadas uma única vez e reutilizadas para calcular weekly trend E day-of-week stats em memória (Java streams)
- **Nunca usar `DAYOFWEEK()` em JPQL/nativeQuery** — é função MySQL. No PostgreSQL usar `EXTRACT(DOW FROM campo_date)` ou agrupar em Java via `LocalDate::getDayOfWeek`
- `bestStreak` é persistido na coluna `streaks.best_streak` e atualizado em tempo real pelo `StreakCalculationService` — não recalcular em runtime

---

## 🤖 Agentes: Casos de Uso Confirmados

| Tarefa | Agentes | Status |
|--------|---------|--------|
| Setup + estrutura Maven | @backend-architect + @senior-developer | ✅ |
| Domain model + JPA entities | @backend-architect + @senior-developer + @database-optimizer | ✅ |
| Auth JWT com TDD | @security-engineer + @backend-architect + @senior-developer + @api-tester | ✅ |
| Import Engine + Strategy Pattern | @backend-architect + @senior-developer + @api-tester | ✅ |
| Endpoints consulta rotina (N+1 fix) | @backend-architect + @senior-developer + @database-optimizer + @api-tester | ✅ |
| Check-in Engine + upsert DailyLog | @backend-architect + @senior-developer + @api-tester | ✅ |
| Reset automático + Streak Engine | @backend-architect + @senior-developer + @api-tester | ✅ |
| Analytics API (streak + heatmap + weekly) | @backend-architect + @senior-developer + @database-optimizer + @api-tester | ✅ |
| Frontend React completo (3 páginas + hooks) | @frontend-developer | ✅ |
| ImportPage + EmptyRoutineState + Polish UX | @frontend-developer | ✅ |
| Docker multi-stage + nginx SPA + README portfólio | @devops-automator + @technical-writer | ✅ |
| CRUD Áreas — TDD (AreaUseCase + AreaController) | @backend-architect + @senior-developer + @api-tester | ✅ |
| CRUD Tarefas — TDD (TaskUseCase + TaskController) | @backend-architect + @senior-developer + @api-tester | ✅ |
| ManagePage — CRUD frontend com modals e dual-panel | @frontend-developer | ✅ |
| Sprint 8 — ResetFrequency (migration, enum, JPA, DTOs, StreakService, testes) | @backend-architect + @senior-developer + @api-tester | ✅ |
| Sprint 8 — Frontend (types, AreaModal selector, AreaManageCard badge) | @frontend-developer | ✅ |
| Sprint 9 — Analytics por área: V9 migration, DTOs, AreaAnalyticsUseCase, testes (TDD) | @backend-architect + @senior-developer + @database-optimizer + @api-tester | ✅ |
| Sprint 9 — Frontend: AreaAnalyticsPage, useAreaAnalytics, StreakCards clicáveis, nova rota | @frontend-developer | ✅ |
| Export CSV + Conversor HabitNow | @backend-architect + @senior-developer + @api-tester + @frontend-developer | ✅ |
| PWA setup + install prompt | @frontend-developer + @devops-automator | ✅ |
| Sprint 12 — Temporal navigation (date param + DateNavBar + useDay) | @backend-architect + @senior-developer + @api-tester + @frontend-developer | ✅ |
| Single Tasks (backend + frontend) | @backend-architect + @senior-developer + @database-optimizer + @api-tester + @frontend-developer | ✅ |
| Sprint 14 — ScheduleType + dayOfMonth (V11 migration, TDD, frontend toggle) | @backend-architect + @senior-developer + @api-tester + @frontend-developer | ✅ |
| Sprint 15 — Import MERGE mode (TDD backend, modal frontend, toast detalhado) | @backend-architect + @senior-developer + @api-tester + @frontend-developer | ✅ |

---

## 📚 Regras de Negócio Relevantes

1. **Reset diário**: checkboxes resetam à meia-noite. O `DailyLog` do dia anterior é preservado com status final.
2. **Streak**: conta dias consecutivos onde pelo menos 1 tarefa foi concluída na área. Dia sem nenhuma tarefa agendada não quebra o streak.
3. **Importação de rotina**: um usuário pode ter apenas 1 rotina ativa por vez. Importar nova rotina desativa a anterior.
4. **Histórico imutável**: `DailyLog` nunca é deletado. É a fonte de verdade para todos os analytics.
5. **Área sem tarefas no dia**: não aparece na tela "Hoje" — só áreas com tarefas agendadas para aquele dia da semana.
6. **Reordenação**: `PATCH /areas/reorder` e `PATCH /areas/{id}/tasks/reorder` recebem lista completa de IDs. O backend re-atribui `orderIndex` sequencialmente pela posição na lista.
7. **Exclusão de task não afeta DailyLog via código**: o banco faz o cascade. `DailyLog` é auditoria imutável — nunca excluir por código Java.
8. **Ownership check por query, não por lógica de controle**: `findByIdAndUserId` / `findByIdAndArea_User_Id` retornam `Optional.empty()` para IDs inexistentes E para IDs de outros usuários — ambos resultam em 404.
9. **ResetFrequency por área**: cada área tem sua própria frequência de avaliação de streak. DAILY = avalia todo dia (padrão). WEEKLY = avalia apenas na segunda-feira. MONTHLY = avalia apenas no dia 1 do mês. Dias fora da janela de avaliação não quebram nem incrementam o streak — a área é simplesmente ignorada pelo `StreakCalculationService`.
10. **ResetFrequency default**: valor ausente no request (null) é normalizado para DAILY no `AreaUseCase`. O `AreaJpaEntity` usa `@Builder.Default` para garantir DAILY mesmo quando o builder não recebe a propriedade — evita NPE nos testes existentes.
11. **Single Tasks — tarefas one-time**: Completar arquiva automaticamente (archivedAt preenchido). Não participam do reset diário — são globais do usuário. Sem dueDate: aparecem na TodayPage todos os dias até serem marcadas. Com dueDate vencida: aparecem com flag `isOverdue=true`. `uncompleteSingleTask` reverte para pendente (para erros do usuário). Checkbox circular para diferenciar de recurring tasks (quadradas). Tabela `single_tasks` com índice parcial `WHERE completed = FALSE` para queries de pendentes.
12. **ScheduleType em tasks**: `DAY_OF_WEEK` (padrão, original) ou `DAY_OF_MONTH`. Tasks DAY_OF_WEEK usam `dayOfWeek` (não nulo) e `dayOfMonth` = null. Tasks DAY_OF_MONTH usam `dayOfMonth` (1–31) e `dayOfWeek` = null. `TaskUseCase.validateSchedule()` garante a consistência — `@Builder.Default scheduleType = DAY_OF_WEEK` garante backward compat com rotinas importadas antes do Sprint 14.
13. **taskAppliesOnDate() — filtro central**: `GetDayScheduleUseCase.taskAppliesOnDate(TaskJpaEntity, LocalDate)` é o único ponto que decide se uma task aparece num dado dia. DAY_OF_WEEK: compara `task.dayOfWeek == date.getDayOfWeek()`. DAY_OF_MONTH: compara `task.dayOfMonth == date.getDayOfMonth()` E verifica que o mês tem aquele dia (`date.lengthOfMonth()`). Reutilizado por `GetDailyProgressUseCase` e `StreakCalculationService` via referência estática — nunca duplicar essa lógica.
16. **Import MERGE — dedup de tarefas**: chave de duplicata = `title.trim().lower() + "|" + scheduleType + "|" + dayOfWeek + "|" + dayOfMonth`. Tasks de YAML são sempre `DAY_OF_WEEK`. Comparação de nome de área: `name.trim().toLowerCase()`. MERGE com rotina ativa: nunca chama `deactivateAllByUserId`, nunca salva `RoutineJpaEntity`. MERGE sem rotina ativa: comporta como REPLACE mas retorna `mode=MERGE`.
17. **Import REPLACE — response extendido**: `mode`, `areasCreated`, `areasMerged=0`, `tasksCreated`, `tasksSkipped=0`. Campo `totalAreas=areasCreated` e `totalTasks=tasksCreated` para retrocompat.
18. **?mode param — default REPLACE**: `@RequestParam(value = "mode", defaultValue = "REPLACE") ImportMode mode`. String inválida → Spring retorna 400 via `DefaultHandlerExceptionResolver`.

14. **GetDayScheduleUseCase — assinatura com LocalDate**: recebe `LocalDate` (não `DayOfWeek`) para poder avaliar DAY_OF_MONTH corretamente. `RoutineController.getDaySchedule(DayOfWeek)` converte para LocalDate via `TemporalAdjusters.previousOrSame(MONDAY).plusDays(dayOfWeek.getValue() - 1)` — mapeia o dia da semana para a data real na semana ISO atual.
15. **Repositório carrega todas as tasks, filtro em Java**: `findAreasWithTasksByRoutineIdOrderByOrderIndex(routineId)` carrega tudo; o filtro por dia é aplicado em Java via `taskAppliesOnDate()`. A query antiga `findAreasWithTasksByRoutineIdAndDay(routineId, dayOfWeek)` filtrava no banco mas não suportava DAY_OF_MONTH — foi abandonada para operações de leitura.

---

## 🔗 Dependências e Integrações Relevantes

| Dependência | Versão | Uso | Observação |
|---|---|---|---|
| Spring Boot | 3.3.x | Framework base | |
| Spring Security | 6.x | Auth + JWT | |
| Flyway | 9.x | DB migrations | |
| Testcontainers | 1.19.x | Testes integração | Requer Docker rodando |
| SnakeYAML | 2.x | Parse de YAML | Já incluso no Spring Boot |
| JJWT | 0.12.x | JWT tokens | `io.jsonwebtoken` |
| React | 18.x | Frontend | |
| shadcn/ui | latest | Componentes UI | |
| Tailwind CSS | 3.x | Estilização | |
| Recharts | 3.x | Gráficos | Bundle ~222KB gzipped — não code-split (aceitável para portfolio) |

---

## 📝 Changelog do CLAUDE.md

| Data | Versão | O que mudou |
|---|---|---|
| 2026-04-19 | 1.0.0 | Criação inicial — arquitetura, ADRs, padrões, stack definidos |
| 2026-04-19 | 1.1.0 | Sprint 1 concluído — agentes confirmados, erro Docker documentado |
| 2026-04-19 | 1.2.0 | Sprint 2 concluído — Import Engine, endpoints rotina, erro TxtParser documentado |
| 2026-04-19 | 1.3.0 | Sprint 3 concluído — Check-in, reset @Scheduled, streak engine, padrão streak documentado |
| 2026-04-19 | 1.4.0 | Sprint 4 concluído — Analytics API: Streak, Heatmap, Weekly (completion, comparison, history) |
| 2026-04-19 | 1.5.0 | Sprint 5 iniciado — Frontend: Vite+React+TS, Tailwind dark, shadcn/ui, api.ts, AppLayout, LoginPage |
| 2026-04-19 | 1.5.1 | Sprint 5 P2 — TodayPage: useToday hook, AreaCard, TaskItem, optimistic update, skeleton loading |
| 2026-04-19 | 1.6.0 | Sprint 5 concluído — WeekPage (8 queries paralelas, grid 7×N, hoje destacado), AnalyticsPage (StreakCards, HeatmapGrid CSS, LineChart Recharts), padrão completed-por-área documentado |
| 2026-04-19 | 1.7.0 | Sprint 6 concluído — ImportPage (drag-and-drop nativo, validação de extensão, first-login welcome), EmptyRoutineState (3 páginas), page transitions, Dockerfiles multi-stage, nginx SPA, docker-compose.yml completo, README profissional |
| 2026-04-20 | 1.8.0 | Sprint 7 concluído — CRUD áreas e tarefas com TDD (16 unit + 15 integration tests), V7 migration (order_index em areas), ManagePage dual-panel, ADR-006 ownership 404, correção Spring Security 6 (401 vs 403), padrão deleteTask cascade |
| 2026-04-23 | 1.9.0 | Correções pós-deploy — heatmap Math.ceil (semana parcial), CORS Vercel + shouldNotFilter JWT, DAY_LABELS TS6133, 403 auth endpoints produção |
| 2026-04-23 | 2.0.0 | Sprint 8 concluído — ResetFrequency por área (V8 migration, enum, JPA @Builder.Default, DTOs, AreaUseCase, StreakCalculationService shouldEvaluateStreak), testes WEEKLY/MONTHLY (unit + integration), frontend AreaModal selector + AreaManageCard badge, regras de negócio 9-10 |
| 2026-04-24 | 2.1.0 | Sprint 9 concluído — Analytics individual por área: V9 migration (best_streak), StreakJpaEntity.bestStreak, DayOfWeekStat + WeeklyTrendPoint + AreaAnalyticsResponse DTOs, AreaAnalyticsUseCase, AreaController /analytics endpoint, 8 unit tests + 4 integration tests, frontend AreaAnalyticsPage (4 summary cards + LineChart + BarChart horizontal), StreakCards clicáveis, rota analytics/area/:areaId, fix LabelFormatter TS2322 (v: unknown), DAYOFWEEK PostgreSQL erro documentado |
| 2026-04-24 | 2.2.0 | Sprint 10 concluído — Export CSV (ExportUseCase + ExportController, BOM UTF-8, StreamingResponseBody, range 365d, 5 unit + 5 integration tests), HabitNow Converter (habitnow-parser.ts, HabitNowConverterPage, geração YAML frontend-only), botão Export CSV na AnalyticsPage, link na ImportPage, padrões 12-13 documentados |
| 2026-04-24 | 2.3.0 | Sprint 11 concluído — PWA: vite-plugin-pwa (autoUpdate, NetworkOnly API), manifest com ícones 192/512/maskable, sw.js + workbox, meta tags iOS Safari, InstallPrompt (mobile-only, dispensável, beforeinstallprompt), vercel.json headers sw.js/manifest, react-is instalado, fix recharts rolldown |
| 2026-04-24 | 2.4.0 | Sprint 12 concluído — Navegação temporal: CheckInUseCase rejeita datas futuras (IllegalArgumentException → 400), CheckInController ?date= param em /complete /uncomplete /progress, alias /today/progress mantido, GET /checkins/progress novo endpoint, 7 unit tests + 4 integration tests novos (156 total), frontend: useDay hook (selectedDate, reset em mudança de data), DateNavBar (14 dias, pills com today highlight, dots via queryClient cache), TodayPage (DateNavBar integrado, label dinâmico, banner futuro, disabled prop), AreaCard + TaskItem (disabled prop), api.ts (complete/uncomplete/getDayProgress aceitam date opcional) |
| 2026-04-24 | 2.5.0 | Sprint 13 concluído — Single Tasks: V10 migration (single_tasks + índice parcial WHERE completed=FALSE), SingleTask domain record, SingleTaskJpaEntity (Long userId direto), SingleTaskJpaRepository (findPendingByUserId NULLS LAST, findArchivedByUserId), CreateSingleTaskRequest + SingleTaskResponse DTOs, SingleTaskUseCase (create/complete/uncomplete/delete/listPending/listArchived), SingleTaskController (POST /single-tasks, GET /single-tasks, /archived, /today, /complete, /uncomplete, DELETE), GlobalExceptionHandler IllegalStateException → 409, 10 unit tests + 11 integration tests (177 total), frontend: tipos SingleTaskResponse/CreateSingleTaskRequest, singleTaskApi, useSingleTasks (5 hooks com optimistic update), SingleTaskItem (circular checkbox, fade-out 280ms, isOverdue badge, delete X), CreateSingleTaskModal (Dialog shadcn), TodayPage seção "Para fazer" (só hoje), SingleTasksPage (Tabs Pendentes/Arquivadas, grupos Atrasadas/Hoje/Sem prazo/Futuras, desfazer), NavBar atualizado (CheckSquare, Importar removido do mobile) |
| 2026-04-30 | 2.7.0 | Sprint 15 concluído — Import MERGE mode: ImportMode enum (REPLACE/MERGE), ImportRoutineResponse +5 campos (mode/areasCreated/areasMerged/tasksCreated/tasksSkipped), ImportRoutineUseCase refatorado (executeReplace/executeMerge privados, dedup key title+scheduleType+dayOfWeek+dayOfMonth), RoutineController ?mode param (defaultValue=REPLACE), fixture merge_routine.yaml, 10 unit tests ImportRoutineUseCaseTest + 4 integration tests RoutineControllerTest (170 total), frontend: ImportMode/ImportRoutineResponse em types, routineApi.importRoutine(mode), useImportRoutine toast detalhado por mode, ImportPage modal REPLACE/MERGE com ModeCard, HabitNowConverterPage tip sobre MERGE. ADR-007 + regras 16-18 |
| 2026-04-30 | 2.6.0 | Sprint 14 concluído — ScheduleType (DAY_OF_WEEK \| DAY_OF_MONTH): V11 migration (day_of_week nullable, schedule_type NOT NULL DEFAULT 'DAY_OF_WEEK', day_of_month INT, CHECK constraints), ScheduleType enum, Task domain record atualizado, TaskJpaEntity (@Builder.Default scheduleType=DAY_OF_WEEK), CreateTaskRequest/UpdateTaskRequest/TaskResponse com scheduleType+dayOfMonth, TaskUseCase.validateSchedule() cross-field, GetDayScheduleUseCase assinatura LocalDate + taskAppliesOnDate() public static + filtro Java-side, GetDailyProgressUseCase + StreakCalculationService migrados para filtro Java, RoutineController getDaySchedule usa TemporalAdjusters ISO week, 13 unit tests TaskUseCaseTest + 4 GetDayScheduleUseCaseTest + 5 GetDailyProgressUseCaseTest + 9 StreakCalculationServiceTest, 9 TaskControllerTest integration — 160 total. Frontend: ScheduleType type, TaskResponse/CreateTaskRequest/UpdateTaskRequest atualizados, TaskManageRow scheduleLabel() (Seg / D25), ManagePage TaskModal toggle DAY_OF_WEEK/DAY_OF_MONTH + dayOfMonth input, agrupamento "Mensal" separado. Fix UnnecessaryStubbingException (stubs removidos de 4 testes de validação). Regras de negócio 12–15 documentadas. |
