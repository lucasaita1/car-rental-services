#!/usr/bin/env python3
"""
Lê um relatório jacoco.xml e imprime a porcentagem de cobertura de LINHAS
(agregada no nível raiz do relatório, ou seja, do módulo inteiro).

Uso: parse-jacoco.py caminho/para/jacoco.xml
Se o arquivo não existir ou não puder ser lido, não imprime nada (string vazia)
e sai com código 1, para o chamador tratar como "cobertura não disponível".

Observação: xml.etree.ElementTree (via expat) não resolve DTDs externas por
padrão, então o DOCTYPE do jacoco.xml (que aponta para report.dtd) não causa
nenhuma tentativa de acesso à rede durante o parse.
"""
import sys
import xml.etree.ElementTree as ET


def main():
    if len(sys.argv) != 2:
        print("Uso: parse-jacoco.py <jacoco.xml>", file=sys.stderr)
        sys.exit(1)

    path = sys.argv[1]

    try:
        tree = ET.parse(path)
    except (FileNotFoundError, ET.ParseError) as err:
        print(f"Não foi possível ler {path}: {err}", file=sys.stderr)
        sys.exit(1)

    root = tree.getroot()

    for counter in root.findall("counter"):
        if counter.get("type") == "LINE":
            missed = int(counter.get("missed", "0"))
            covered = int(counter.get("covered", "0"))
            total = missed + covered
            pct = round((covered / total) * 100, 2) if total else 0.0
            print(pct)
            return

    print("Nenhum counter do tipo LINE encontrado no relatório.", file=sys.stderr)
    sys.exit(1)


if __name__ == "__main__":
    main()
