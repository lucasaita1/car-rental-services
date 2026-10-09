#!/usr/bin/env bash
# Agente de IA que ALTERA CÓDIGO do projeto e prepara um Pull Request.
#
# Diferente dos agentes de observabilidade (que só leem logs/métricas/traces
# e abrem issue), este agente escreve código: ele recebe uma classe Java,
# devolve a versão melhorada (Javadoc em todos os métodos públicos, sem
# tocar na lógica) e gera o título e o corpo do PR que o workflow vai abrir.
#
# Reusa scripts/ask-ai.sh (Groq, mesma infraestrutura da Aula 3).
set -euo pipefail

TARGET="car-microservice/src/main/java/dev/lucas/car_microservice/service/CarService.java"

SYSTEM_PROMPT="Você é um agente autônomo de melhoria de código Java integrado a um pipeline CI/CD.
Sua tarefa: adicionar Javadoc em português do Brasil à classe e a TODOS os métodos públicos do arquivo recebido.
Regras obrigatórias:
1. Devolva o ARQUIVO COMPLETO, do package à última chave.
2. Responda SOMENTE com código Java. Sem cercas de markdown, sem explicações antes ou depois.
3. NÃO altere nenhuma lógica, assinatura de método, import ou anotação existente.
4. O Javadoc de cada método deve explicar o que ele faz e, quando houver, as exceções lançadas (@throws).
5. Mantenha a formatação e a indentação originais do restante do código."

echo "== Agente de IA: lendo ${TARGET} =="
CODE=$(cat "$TARGET")

echo "== Agente de IA: solicitando a alteração ao modelo =="
RESPOSTA=$(bash scripts/ask-ai.sh "$SYSTEM_PROMPT" "$CODE")

# Remove cercas de markdown caso o modelo desobedeça a regra 2.
echo "$RESPOSTA" | python3 -c '
import sys
text = sys.stdin.read().strip()
if text.startswith("```"):
    lines = text.splitlines()[1:]
    while lines and lines[-1].strip().startswith("```"):
        lines.pop()
    text = "\n".join(lines).strip()
print(text)
' > "$TARGET"

# Sanidade: o resultado precisa continuar sendo a mesma classe Java.
head -1 "$TARGET" | grep -q "^package dev.lucas.car_microservice.service;" || {
  echo "ERRO: a resposta da IA não parece um arquivo Java válido. Abortando sem abrir PR." >&2
  exit 1
}
grep -q "class CarService" "$TARGET" || {
  echo "ERRO: a resposta da IA não contém a classe CarService. Abortando sem abrir PR." >&2
  exit 1
}

echo "== Alteração aplicada pelo agente =="
git diff --stat -- "$TARGET"

if git diff --quiet -- "$TARGET"; then
  echo "ERRO: a IA não alterou nada no arquivo. Abortando sem abrir PR." >&2
  exit 1
fi

echo "== Agente de IA: redigindo o título e o corpo do Pull Request =="
DIFF=$(git diff -- "$TARGET" | head -300)
PR_SYSTEM="Você escreve descrições de Pull Request em português do Brasil.
Receberá um diff de código. Responda EXATAMENTE neste formato:
- Primeira linha: título curto do PR (máximo 70 caracteres, começando com 'docs(car): ').
- Segunda linha: em branco.
- Demais linhas: corpo do PR em markdown, explicando o que o agente de IA alterou e por quê, em até 10 linhas."
PR_TEXT=$(bash scripts/ask-ai.sh "$PR_SYSTEM" "$DIFF")

echo "$PR_TEXT" | head -1 > pr-title.txt
echo "$PR_TEXT" | tail -n +3 > pr-body.md

# Garantias caso o modelo fuja do formato.
[ -s pr-title.txt ] || echo "docs(car): adiciona Javadoc ao CarService (agente de IA)" > pr-title.txt
[ -s pr-body.md ] || echo "Javadoc adicionado automaticamente pelo agente de IA." > pr-body.md
{
  echo ""
  echo "---"
  echo "_PR aberto automaticamente pelo workflow **Agente de IA - Pull Request** (run ${GITHUB_RUN_ID:-local}). A alteração foi gerada por IA e validada com \`mvn compile\` antes deste PR ser criado._"
} >> pr-body.md

echo "== Título do PR =="
cat pr-title.txt
echo "== Corpo do PR =="
cat pr-body.md
