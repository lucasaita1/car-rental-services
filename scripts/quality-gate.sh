#!/usr/bin/env bash
# Quality Gate com IA (Atividade 1)
#
# Lê a cobertura de linhas gerada pelo JaCoCo para o módulo car-microservice
# e pergunta à IA se o pipeline pode avançar. Sai com código 1 (falha o step)
# se a IA responder BLOQUEADO — isso impede o job de terminar verde.
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JACOCO_XML="car-microservice/target/site/jacoco/jacoco.xml"

COVERAGE=""
if COVERAGE_RAW=$(python3 "$DIR/parse-jacoco.py" "$JACOCO_XML" 2>/dev/null); then
  COVERAGE="${COVERAGE_RAW}%"
else
  echo "Aviso: não foi possível ler a cobertura em $JACOCO_XML" >&2
  COVERAGE="não disponível"
fi

SYSTEM_PROMPT="Você é um quality gate de pipeline CI/CD para um microsserviço Java/Spring Boot (car-microservice), medido com JaCoCo.
Regras de decisão:
- Cobertura de linhas abaixo de 70% deve BLOQUEAR o pipeline.
- Cobertura entre 70% e 85% é aceitável, mas aprove citando o que priorizar como ressalva.
- Cobertura acima de 85% é APROVADO sem ressalvas.
- Se a cobertura estiver marcada como 'não disponível', BLOQUEIE e peça para corrigir a geração do relatório JaCoCo.
Responda SEMPRE começando a primeira linha com a palavra APROVADO ou BLOQUEADO (maiúsculas), seguida de um traço e uma justificativa curta (1 a 3 frases)."

USER_PROMPT="Cobertura de linhas (JaCoCo) do módulo car-microservice: ${COVERAGE}

Avalie se este pipeline pode avançar para deploy."

echo "Cobertura enviada para a IA: $COVERAGE"
echo ""

DECISION=$(bash "$DIR/ask-ai.sh" "$SYSTEM_PROMPT" "$USER_PROMPT")

echo "Decisão da IA:"
echo "$DECISION"
echo ""

if echo "$DECISION" | head -n1 | grep -qi "BLOQUEADO"; then
  echo "Quality Gate BLOQUEOU o pipeline." >&2
  exit 1
fi

echo "Quality Gate APROVOU o pipeline."
