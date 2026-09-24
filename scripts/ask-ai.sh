#!/usr/bin/env bash
# Uso: ask-ai.sh "<system prompt>" "<user prompt>"
# Imprime no stdout SOMENTE o texto da decisão devolvida pela IA.
#
# Usa Groq por padrão (GROQCLOUD_API_KEY, camada gratuita); cai para OpenAI
# se OPENAI_API_KEY estiver definida e a da Groq não.
#
# O corpo da requisição é montado com python (json.dumps) em vez de
# concatenar strings manualmente no curl -d '...' — isso evita o problema
# de JSON quebrado quando o log/métrica/trace contém aspas, quebras de
# linha ou barras invertidas (ver seção 3.2 do roteiro da aula).
set -euo pipefail

SYSTEM_PROMPT="${1:?informe o system prompt}"
USER_PROMPT="${2:?informe o user prompt}"

# A conta da Groq pode não ter acesso ao modelo do roteiro
# (llama-3.1-8b-instant). Por isso tentamos, em ordem, os modelos
# disponíveis na conta até um responder com conteúdo.
if [ -n "${GROQCLOUD_API_KEY:-}" ]; then
  URL="https://api.groq.com/openai/v1/chat/completions"
  API_KEY="$GROQCLOUD_API_KEY"
  MODELS="openai/gpt-oss-20b openai/gpt-oss-120b qwen/qwen3.8-27b allam-2-7b llama-3.1-8b-instant"
elif [ -n "${OPENAI_API_KEY:-}" ]; then
  URL="https://api.openai.com/v1/chat/completions"
  API_KEY="$OPENAI_API_KEY"
  MODELS="gpt-4o-mini"
else
  echo "Erro: defina GROQCLOUD_API_KEY (ou OPENAI_API_KEY) como secret do repositório." >&2
  exit 1
fi

for MODEL in $MODELS; do
  BODY=$(python3 -c '
import json, sys
system_prompt, user_prompt, model = sys.argv[1], sys.argv[2], sys.argv[3]
print(json.dumps({
    "model": model,
    "messages": [
        {"role": "system", "content": system_prompt},
        {"role": "user", "content": user_prompt},
    ],
}))
' "$SYSTEM_PROMPT" "$USER_PROMPT" "$MODEL")

  RESPONSE=$(curl -s "$URL" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $API_KEY" \
    -d "$BODY")

  if CONTENT=$(echo "$RESPONSE" | python3 -c "
import sys, json
r = json.load(sys.stdin)
try:
    content = r['choices'][0]['message']['content']
except (KeyError, IndexError, TypeError):
    print('ERRO_IA: modelo $MODEL falhou -> ' + json.dumps(r), file=sys.stderr)
    sys.exit(1)
if not content or not content.strip():
    print('ERRO_IA: modelo $MODEL devolveu conteudo vazio', file=sys.stderr)
    sys.exit(1)
print(content)
"); then
    echo "$CONTENT"
    exit 0
  fi
done

echo "ERRO_IA: nenhum dos modelos respondeu com sucesso (tentados: $MODELS)." >&2
exit 1
