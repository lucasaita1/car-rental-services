#!/usr/bin/env bash
# IA analisando logs (Atividade 2 / Agente de Incidente)
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_CONTENT=$(cat logs/app.log)

SYSTEM_PROMPT="Você é um especialista em observabilidade e análise de logs de um microsserviço Java/Spring Boot (car-microservice).
Analise os logs e diga claramente se existe algum problema CRÍTICO que exija abertura de incidente (ex: falha ao processar pagamento/reserva, indisponibilidade de banco de dados, erro em cascata).
Responda de forma objetiva. Se houver algo crítico, use explicitamente a palavra 'crítico' na resposta."

USER_PROMPT="Analise estes logs e diga se existe erro crítico:

$LOG_CONTENT"

echo "Logs analisados:"
echo "$LOG_CONTENT"
echo ""

DECISION=$(bash "$DIR/ask-ai.sh" "$SYSTEM_PROMPT" "$USER_PROMPT")

echo "Análise da IA:"
echo "$DECISION"

echo "$DECISION" > ia-decision.txt

if echo "$DECISION" | grep -qi "crítico\|critico"; then
  echo "" >&2
  echo "Problema crítico detectado nos logs." >&2
  exit 1
fi

echo ""
echo "Nenhum problema crítico detectado."
