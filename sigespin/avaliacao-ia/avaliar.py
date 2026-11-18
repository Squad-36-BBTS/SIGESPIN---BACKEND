"""Avalia a API em execução com casos fictícios. Usa apenas a biblioteca padrão.

Defina SIGESPIN_API_URL, SIGESPIN_AVALIADOR_EMAIL e SIGESPIN_AVALIADOR_SENHA.
A chave Gemini fica exclusivamente no ambiente do backend.
"""
import json
import os
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


def requisicao(base, rota, corpo, token=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    req = Request(base + rota, data=json.dumps(corpo).encode(), headers=headers)
    with urlopen(req, timeout=30) as resposta:
        return json.load(resposta)


def estrutura_valida(resposta):
    campos = {"arenaSugerida", "hipotese", "trlInicial", "potencialLeiDoBem", "justificativaArena", "scoresRadar"}
    if not isinstance(resposta, dict) or set(resposta) != campos:
        return False
    if resposta["arenaSugerida"] not in {"EFICIENCIA_OPERACIONAL", "NEGOCIOS_DIGITAIS", "SEGURANCA_INTEGRADA", "A_DEFINIR"}:
        return False
    if type(resposta["trlInicial"]) is not int or not 1 <= resposta["trlInicial"] <= 3:
        return False
    if type(resposta["potencialLeiDoBem"]) is not bool:
        return False
    if any(not isinstance(resposta[campo], str) or not resposta[campo].strip() or len(resposta[campo]) > 1000
           for campo in ("hipotese", "justificativaArena")):
        return False
    radar = resposta["scoresRadar"]
    return (isinstance(radar, dict) and set(radar) == {"negocio", "produto", "time", "receita"}
            and all(type(nota) in (int, float) and 0 <= nota <= 4 for nota in radar.values()))


def main():
    base = os.environ["SIGESPIN_API_URL"].rstrip("/")
    if not base.startswith(("http://127.0.0.1:", "http://localhost:", "https://")):
        raise ValueError("Use localhost ou HTTPS para proteger a autenticação.")
    token = requisicao(base, "/api/auth/login", {
        "email": os.environ["SIGESPIN_AVALIADOR_EMAIL"], "senha": os.environ["SIGESPIN_AVALIADOR_SENHA"]})["token"]
    corpus = json.loads(Path(__file__).with_name("casos-sinteticos.json").read_text(encoding="utf-8"))
    resultados = []
    for caso in corpus["casos"]:
        item = {"id": caso["id"], "arenaEsperada": caso["arenaEsperada"], "revisaoHumana": "PENDENTE"}
        try:
            resposta = requisicao(base, "/api/ia/analisar-demanda", {
                "titulo": caso["titulo"], "descricao": caso["descricao"]}, token)
            item.update(resposta=resposta, contratoValido=estrutura_valida(resposta),
                        arenaCorresponde=None if caso["arenaEsperada"] is None else resposta.get("arenaSugerida") == caso["arenaEsperada"])
        except HTTPError as erro:
            item["httpStatus"] = erro.code  # Não expõe token, senha ou corpo de erro do provedor.
        except URLError:
            item["erro"] = "API indisponível"
        resultados.append(item)
    destino = Path(__file__).parent.parent / "target" / "avaliacao-ia.json"
    destino.parent.mkdir(parents=True, exist_ok=True)
    destino.write_text(json.dumps({"natureza": corpus["natureza"], "resultados": resultados}, ensure_ascii=False, indent=2), encoding="utf-8")
    print("Relatório:", destino)
    print("A validação automática não substitui a revisão de pertinência, evidências e incerteza pelo cliente.")
    return 0 if all(r.get("contratoValido") and r.get("arenaCorresponde") is not False for r in resultados) else 1


if __name__ == "__main__":
    raise SystemExit(main())
