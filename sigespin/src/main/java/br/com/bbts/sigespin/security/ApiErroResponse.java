package br.com.bbts.sigespin.security;

import java.time.Instant;
import java.util.List;

public record ApiErroResponse(Instant timestamp, int status, String codigo, String mensagem, List<String> detalhes) { }
