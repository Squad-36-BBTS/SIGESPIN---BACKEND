#!/usr/bin/env python3
"""Verificação rápida do backend publicado (BE-11).

Uso, a partir da pasta sigespin:

    1ª execução (logo após o deploy):
        python scripts/verificar_deploy.py

    2ª execução (depois de reiniciar o serviço no Render):
        python scripts/verificar_deploy.py --apos-reinicio

Variáveis de ambiente:
    SIGESPIN_API_URL           ex.: https://sigespin-backend.onrender.com (sem /api no final)
    SMOKE_DEMANDANTE_EMAIL / SMOKE_DEMANDANTE_SENHA
    SMOKE_ANALISTA_EMAIL   / SMOKE_ANALISTA_SENHA
    SMOKE_ESPECIALISTA_EMAIL / SMOKE_ESPECIALISTA_SENHA
    SMOKE_TESTAR_IA=1          (opcional) também chama a sugestão da IA

A 1ª execução cria um projeto "[VERIFICACAO]" e envia um PDF pequeno.
A 2ª confere se o mesmo arquivo continua disponível depois do reinício
(prova de que os comprovantes não ficam no disco do Render) e arquiva o projeto.
Use apenas dados fictícios. Só depende da biblioteca padrão do Python.
"""
import hashlib
import json
import os
import sys
import time
import urllib.error
import urllib.request
import uuid

ESTADO = os.path.join("target", "verificacao-deploy.json")
PERFIS = ("DEMANDANTE", "ANALISTA", "ESPECIALISTA")
falhas = []


def api_base():
    url = os.environ.get("SIGESPIN_API_URL", "").rstrip("/")
    if not url:
        sys.exit("Defina SIGESPIN_API_URL (ex.: https://sigespin-backend.onrender.com).")
    if url.endswith("/api"):
        url = url[:-4]
    return url


def chamar(metodo, caminho, token=None, corpo=None, tipo="application/json", tempo=90):
    """Faz a requisição e devolve (status, corpo em bytes, cabeçalhos)."""
    dados = corpo if isinstance(corpo, (bytes, type(None))) else json.dumps(corpo).encode()
    req = urllib.request.Request(BASE + caminho, data=dados, method=metodo)
    if dados is not None:
        req.add_header("Content-Type", tipo)
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=tempo) as resp:
            return resp.status, resp.read(), resp.headers
    except urllib.error.HTTPError as erro:
        return erro.code, erro.read(), erro.headers


def conferir(descricao, condicao, detalhe=""):
    print(("  OK    " if condicao else "  FALHA ") + descricao + ("" if condicao else f" — {detalhe}"))
    if not condicao:
        falhas.append(descricao)
    return condicao


def aguardar_servico():
    # O Render gratuito dorme após 15 minutos: a primeira chamada pode levar perto de um minuto.
    print("Acordando o serviço (pode levar até 1 minuto no plano gratuito)...")
    for _ in range(12):
        try:
            status, corpo, _ = chamar("GET", "/actuator/health", tempo=20)
            if status == 200 and b'"UP"' in corpo:
                return conferir("/actuator/health responde UP", True)
        except Exception:
            pass
        time.sleep(5)
    conferir("/actuator/health responde UP", False, "o serviço não respondeu em 1 minuto")
    sys.exit(1)


def entrar(perfil):
    email = os.environ.get(f"SMOKE_{perfil}_EMAIL")
    senha = os.environ.get(f"SMOKE_{perfil}_SENHA")
    if not email or not senha:
        sys.exit(f"Defina SMOKE_{perfil}_EMAIL e SMOKE_{perfil}_SENHA.")
    status, corpo, _ = chamar("POST", "/api/auth/login", corpo={"email": email, "senha": senha})
    ok = conferir(f"login {perfil}", status == 200, f"HTTP {status}")
    return json.loads(corpo)["token"] if ok else None


def pdf_ficticio():
    # PDF mínimo e único a cada execução, para o hash identificar este envio.
    marca = uuid.uuid4().hex
    return (b"%PDF-1.4\n% verificacao " + marca.encode() +
            b"\n1 0 obj<</Type/Catalog>>endobj\ntrailer<</Root 1 0 R>>\n%%EOF\n")


def multipart(nome_arquivo, conteudo, tipo):
    fronteira = "----sigespin" + uuid.uuid4().hex
    corpo = (f"--{fronteira}\r\nContent-Disposition: form-data; name=\"arquivo\"; "
             f"filename=\"{nome_arquivo}\"\r\nContent-Type: {tipo}\r\n\r\n").encode()
    corpo += conteudo + f"\r\n--{fronteira}--\r\n".encode()
    return corpo, "multipart/form-data; boundary=" + fronteira


def primeira_execucao(tokens):
    for perfil in PERFIS:
        status, _, _ = chamar("GET", "/api/projetos?size=1", tokens[perfil])
        conferir(f"portfólio visível para {perfil}", status == 200, f"HTTP {status}")

    status, _, _ = chamar("GET", "/api/projetos?size=1")
    conferir("portfólio sem token é recusado (401)", status == 401, f"HTTP {status}")

    demanda = {"titulo": "[VERIFICACAO] Deploy " + time.strftime("%d/%m %H:%M"),
               "descricaoDor": "Projeto fictício criado pela verificação de deploy.",
               "areaDemandanteId": 1, "origem": "ESPONTANEA", "trlInicialDeclarado": 1}
    status, corpo, _ = chamar("POST", "/api/projetos", tokens["DEMANDANTE"], demanda)
    if not conferir("Demandante registra demanda", status == 201, f"HTTP {status}: {corpo[:200]!r}"):
        return
    projeto_id = json.loads(corpo)["id"]

    conteudo = pdf_ficticio()
    dados, tipo = multipart("verificacao.pdf", conteudo, "application/pdf")
    status, corpo, _ = chamar("POST", f"/api/projetos/{projeto_id}/evidencias", tokens["DEMANDANTE"], dados, tipo)
    if not conferir("upload de comprovante PDF", status == 201, f"HTTP {status}: {corpo[:200]!r}"):
        return
    evidencia = json.loads(corpo)
    esperado = hashlib.sha256(conteudo).hexdigest()
    conferir("SHA-256 registrado confere", evidencia.get("sha256") == esperado)

    status, baixado, cabecalhos = chamar("GET", evidencia["downloadUrl"], tokens["ANALISTA"])
    conferir("download autenticado devolve o mesmo arquivo",
             status == 200 and hashlib.sha256(baixado).hexdigest() == esperado, f"HTTP {status}")
    conferir("download informa o nome do arquivo", "verificacao.pdf" in (cabecalhos.get("Content-Disposition") or ""))
    status, _, _ = chamar("GET", evidencia["downloadUrl"])
    conferir("download sem token é recusado (401)", status == 401, f"HTTP {status}")

    if os.environ.get("SMOKE_TESTAR_IA") == "1":
        status, corpo, _ = chamar("POST", "/api/ia/analisar-demanda", tokens["DEMANDANTE"],
                                  {"titulo": "Conferência manual de documentos",
                                   "descricao": "Caso fictício: a conferência manual gera retrabalho."})
        conferir("sugestão da IA responde 200 (ou 503 se a cota acabou)", status in (200, 503), f"HTTP {status}")
        if status == 503:
            print("        A IA respondeu 503: o preenchimento manual continua disponível.")

    os.makedirs("target", exist_ok=True)
    with open(ESTADO, "w", encoding="utf-8") as arquivo:
        json.dump({"projetoId": projeto_id, "downloadUrl": evidencia["downloadUrl"], "sha256": esperado}, arquivo)
    print(f"\nAgora reinicie o serviço no Render (Manual Deploy → Restart service) e rode com --apos-reinicio.")


def apos_reinicio(tokens):
    if not os.path.exists(ESTADO):
        sys.exit("Rode primeiro sem --apos-reinicio.")
    with open(ESTADO, encoding="utf-8") as arquivo:
        estado = json.load(arquivo)
    status, baixado, _ = chamar("GET", estado["downloadUrl"], tokens["ANALISTA"])
    conferir("comprovante continua disponível após o reinício",
             status == 200 and hashlib.sha256(baixado).hexdigest() == estado["sha256"], f"HTTP {status}")

    status, corpo, _ = chamar("GET", f"/api/projetos/{estado['projetoId']}", tokens["ESPECIALISTA"])
    if status == 200:
        versao = json.loads(corpo).get("versao", 0)
        status, _, _ = chamar("PUT", f"/api/projetos/{estado['projetoId']}/arquivamento", tokens["ESPECIALISTA"],
                              {"arquivado": True, "versao": versao,
                               "justificativa": "Projeto fictício da verificação de deploy."})
        conferir("projeto de verificação arquivado", status == 200, f"HTTP {status}")
    os.remove(ESTADO)


if __name__ == "__main__":
    BASE = api_base()
    print(f"Verificando {BASE}\n")
    aguardar_servico()
    tokens = {perfil: entrar(perfil) for perfil in PERFIS}
    if all(tokens.values()):
        (apos_reinicio if "--apos-reinicio" in sys.argv else primeira_execucao)(tokens)
    print("\nResultado: " + ("tudo certo." if not falhas else f"{len(falhas)} falha(s): " + "; ".join(falhas)))
    sys.exit(1 if falhas else 0)
