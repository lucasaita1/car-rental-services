#!/usr/bin/env bash
# IA analisando traces distribuídos (Atividade 4)
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TRACE=$(tr '\n' ' ' < traces/car_reservation_trace.txt)

SYSTEM_PROMPT="Você é especialista em distributed tracing para uma arquitetura de microsserviços: API Gateway -> car-microservice -> Auth Service -> Payment Service -> PostgreSQL Database.
Analise o trace e identifique gargalos ou timeouts. Se houver, use explicitamente uma das palavras: 'timeout', 'gargalo' ou 'crítico'."

USER_PROMPT="Analise este trace distribuído e identifique gargalos:

$TRACE"

echo "Trace analisado:"
echo "$TRACE"
echo ""

DECISION=$(bash "$DIR/ask-ai.sh" "$SYSTEM_PROMPT" "$USER_PROMPT")

echo "Análise da IA:"
echo "$DECISION"

echo "$DECISION" > ia-decision.txt

if echo "$DECISION" | grep -qi "timeout\|gargalo\|crítico\|critico"; then
  echo "" >&2
  echo "Gargalo/timeout detectado no trace." >&2
  exit 1
fi

echo ""
echo "Nenhum gargalo detectado."
