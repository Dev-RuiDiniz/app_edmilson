# TV Device Limit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enviar `device_id` nas chamadas da API de TV e bloquear a renderização quando o backend sinalizar limite de TVs ou ausência de identificador.

**Architecture:** Um provider dedicado lê o identificador do dispositivo, o repositório passa a incluir `device_id` nas URLs e mapeia respostas de bloqueio para exceções tipadas. A tela de erro atual permanece a mesma, mas recebe mensagens específicas com base na causa.

**Tech Stack:** Kotlin, Android SDK, Retrofit, Gson, JUnit4

---

### Task 1: Documentação de execução

**Files:**
- Create: `docs/superpowers/specs/2026-05-08-tv-device-limit-design.md`
- Create: `docs/superpowers/plans/2026-05-08-tv-device-limit.md`

- [ ] **Step 1: Registrar a spec curta**

Criar a spec com objetivo, decisões, fluxo e mensagens de erro.

- [ ] **Step 2: Registrar o plano**

Criar este plano com tarefas separadas para provider, repositório, UI e validação.

- [ ] **Step 3: Commit**

Run: `git add docs/superpowers/specs/2026-05-08-tv-device-limit-design.md docs/superpowers/plans/2026-05-08-tv-device-limit.md && git commit -m "docs: add tv device limit spec and plan"`

### Task 2: Provider e regras de API

**Files:**
- Create: `app/src/main/java/com/hotspottv/data/device/TvDeviceIdProvider.kt`
- Create: `app/src/main/java/com/hotspottv/data/repository/TvApiException.kt`
- Modify: `app/src/main/java/com/hotspottv/data/repository/TvContentRepository.kt`
- Test: `app/src/test/java/com/hotspottv/data/repository/TvApiContractTest.kt`

- [ ] **Step 1: Escrever os testes de URL e mapeamento**
- [ ] **Step 2: Rodar o teste para ver falhar**
- [ ] **Step 3: Implementar provider, append de `device_id` e exceções tipadas**
- [ ] **Step 4: Rodar os testes do repositório**
- [ ] **Step 5: Commit**

### Task 3: Integração da UI de erro

**Files:**
- Modify: `app/src/main/java/com/hotspottv/ui/renderer/RendererUiState.kt`
- Modify: `app/src/main/java/com/hotspottv/ui/renderer/RendererViewModel.kt`
- Modify: `app/src/main/java/com/hotspottv/RendererActivity.kt`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Escrever o teste da mensagem baseada na causa**
- [ ] **Step 2: Rodar o teste para ver falhar**
- [ ] **Step 3: Implementar causa no estado e mapear mensagens na Activity**
- [ ] **Step 4: Rodar testes unitários relevantes**
- [ ] **Step 5: Commit**

### Task 4: Verificação final, push e instalação

**Files:**
- Modify: `README.md` (somente se necessário para documentar `device_id`)

- [ ] **Step 1: Rodar `.\gradlew.bat testDebugUnitTest assembleDebug`**
- [ ] **Step 2: Revisar `git status` e `git log --oneline -n 5`**
- [ ] **Step 3: Fazer `git push origin <branch>`**
- [ ] **Step 4: Verificar BlueStacks localmente e instalar APK com `adb`**
