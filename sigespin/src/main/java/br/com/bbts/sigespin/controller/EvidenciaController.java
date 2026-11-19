package br.com.bbts.sigespin.controller;

import br.com.bbts.sigespin.dto.entrega.EvidenciaResponse;
import br.com.bbts.sigespin.service.ArquivoEvidenciaService;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/projetos/{projetoId}/evidencias")
@PreAuthorize("isAuthenticated()")
public class EvidenciaController {
    private final ArquivoEvidenciaService arquivos;

    public EvidenciaController(ArquivoEvidenciaService arquivos) { this.arquivos = arquivos; }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EvidenciaResponse> enviar(@PathVariable Long projetoId,
            @RequestPart("arquivo") MultipartFile arquivo) throws IOException {
        var criada = arquivos.enviar(projetoId, arquivo);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}/conteudo").buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(location).body(criada);
    }

    @GetMapping
    public List<EvidenciaResponse> listar(@PathVariable Long projetoId) { return arquivos.listar(projetoId); }

    @GetMapping("/{id}/conteudo")
    public ResponseEntity<Resource> baixar(@PathVariable Long projetoId, @PathVariable Long id) {
        var arquivo = arquivos.baixar(projetoId, id);
        var resposta = ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(arquivo.nome(), StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType(arquivo.contentType()));
        if (arquivo.tamanho() != null) resposta.contentLength(arquivo.tamanho());
        return resposta.body(arquivo.recurso());
    }
}
