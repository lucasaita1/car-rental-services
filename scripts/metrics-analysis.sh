#!/usr/bin/env bash
# IA analisando métricas (Atividade 3)
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
METRICS=$(cat metrics/system.json)

SYSTEM_PROMPT="Você é especialista em observabilidade e análise de métricas de um microsserviço Java/Spring Boot (car-microservice), rodando em JVM.
Analise as métricas informadas (incluindo uso de heap da JVM) e diga se existe risco operacional.
Se houver risco, use explicitamente uma das palavras: 'crítico', 'risco', 'grave' ou 'instável'."

USER_PROMPT="Analise estas métricas e diga se existe risco operacional:
$METRICS"

echo "Métricas analisadas:"
echo "$METRICS"
echo ""

DECISION=$(bash "$DIR/ask-ai.sh" "$SYSTEM_PROMPT" "$USER_PROMPT")

echo "Análise da IA:"
echo "$DECISION"

echo "$DECISION" > ia-decision.txt

if echo "$DECISION" | grep -qi "crítico\|critico\|risco\|grave\|instável\|instavel"; then
  echo "" >&2
  echo "Risco operacional detectado nas métricas." >&2
  exit 1
fi

echo ""
echo "Nenhum risco operacional detectado."
