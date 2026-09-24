# URLs dos prints — Aula 3 (Quality Gate com IA + Observabilidade)

Repositório: https://github.com/lucasaita1/car-rental-services

## Prints 1 e 2 — Quality Gate (`ci.yml`)

| Print | Cenário | Cobertura | URL |
|---|---|---|---|
| 1 | **APROVADO** (com `CarControllerTest`) | 76.84% | https://github.com/lucasaita1/car-rental-services/actions/runs/36071615492 |
| 2 | **BLOQUEADO** (teste do controller desabilitado) | 68.36% | https://github.com/lucasaita1/car-rental-services/actions/runs/36071765016 |

Run extra confirmando a volta ao estado APROVADO após o revert: https://github.com/lucasaita1/car-rental-services/actions/runs/36071868647

## Prints 3 a 8 — Observabilidade (3 workflows + 3 issues)

| Prints | Agente | Run (execução) | Issue criada automaticamente |
|---|---|---|---|
| 3 e 4 | Incidente (logs) | https://github.com/lucasaita1/car-rental-services/actions/runs/36071987928 | https://github.com/lucasaita1/car-rental-services/issues/1 |
| 5 e 6 | Métricas | https://github.com/lucasaita1/car-rental-services/actions/runs/36071990734 | https://github.com/lucasaita1/car-rental-services/issues/2 |
| 7 e 8 | Traces | https://github.com/lucasaita1/car-rental-services/actions/runs/36071993787 | https://github.com/lucasaita1/car-rental-services/issues/3 |

## Observações de adaptação

- **Modelo da Groq:** a conta não tem acesso ao `llama-3.1-8b-instant` do roteiro. O `scripts/ask-ai.sh`
  agora tenta, em ordem, `openai/gpt-oss-20b`, `openai/gpt-oss-120b`, `qwen/qwen3.8-27b`,
  `allam-2-7b` e, por fim, o `llama-3.1-8b-instant` — usando o primeiro que responder.
- **Cobertura:** o projeto estava em 68.36% (abaixo do limite de 70%). Foi adicionado o
  `CarControllerTest` (@WebMvcTest + MockMvc) elevando a cobertura para 76.84%, permitindo o
  cenário APROVADO. No cenário BLOQUEADO, esse teste foi temporariamente anotado com `@Disabled`.
