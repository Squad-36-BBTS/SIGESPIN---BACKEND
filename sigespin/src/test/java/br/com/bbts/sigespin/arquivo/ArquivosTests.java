package br.com.bbts.sigespin.arquivo;

import br.com.bbts.sigespin.exception.*;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import java.io.*;
import java.nio.file.Path;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;

class ArquivosTests {
    @TempDir Path pasta;
    private final ValidadorArquivo validador = new ValidadorArquivo();
    private MockMultipartFile arquivo(String nome, byte[] bytes) {
        return new MockMultipartFile("arquivo", nome, "application/octet-stream", bytes);
    }

    @Test void identificaPdfPeloConteudoESanitizaNome() throws Exception {
        var resultado = validador.validar(arquivo("C:\\fakepath\\comprovante.PDF", "%PDF-1.7\n%%EOF".getBytes()));
        assertEquals("comprovante.PDF", resultado.nome());
        assertEquals("application/pdf", resultado.contentType());
    }
    @Test void aceitaJpeg() throws Exception {
        assertEquals("image/jpeg", validador.validar(arquivo("foto.jpeg", new byte[]{-1,-40,-1,-32,0})).contentType());
    }
    @Test void aceitaWordOle() throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var ole = new POIFSFileSystem()) {
            ole.createDocument(new ByteArrayInputStream(new byte[]{1,2}), "WordDocument");
            ole.writeFilesystem(bytes);
        }
        assertEquals("application/msword", validador.validar(arquivo("relatorio.doc", bytes.toByteArray())).contentType());
    }
    @Test void aceitaWordZip() throws Exception {
        assertTrue(validador.validar(arquivo("relatorio.docx", zip(true))).contentType().contains("wordprocessingml"));
    }
    @Test void rejeitaZipRenomeadoParaWord() throws Exception {
        assertThrows(ArquivoInvalidoException.class, () -> validador.validar(arquivo("relatorio.docx", zip(false))));
    }
    @Test void rejeitaConteudoIncompativelComExtensao() {
        assertThrows(ArquivoInvalidoException.class, () -> validador.validar(arquivo("prova.pdf", "executavel".getBytes())));
        assertThrows(ArquivoInvalidoException.class, () -> validador.validar(arquivo("prova.doc", "%PDF-1.7".getBytes())));
        assertThrows(ArquivoInvalidoException.class, () -> validador.validar(arquivo("prova.jpg", "%PDF-1.7".getBytes())));
    }
    @Test void rejeitaVazioEFormatoNaoPermitido() {
        assertThrows(ArquivoInvalidoException.class, () -> validador.validar(arquivo("vazio.pdf", new byte[0])));
        assertThrows(ArquivoInvalidoException.class, () -> validador.validar(arquivo("planilha.xlsx", new byte[]{1})));
    }
    @Test void aceitaExatamenteDezMbRejeitaUmByteAcima() throws Exception {
        byte[] bytes = new byte[(int) ValidadorArquivo.LIMITE];
        System.arraycopy("%PDF-".getBytes(),0,bytes,0,5);
        assertEquals(bytes.length, validador.validar(arquivo("limite.pdf", bytes)).conteudo().length);
        assertThrows(MaxUploadSizeExceededException.class, () -> validador.validar(arquivo("acima.pdf", new byte[bytes.length+1])));
    }
    @Test void limiteTambemConfereStreamSemConfiarNoTamanhoInformado() {
        var mentiroso = new MockMultipartFile("arquivo", "prova.pdf", "application/pdf", new byte[(int)ValidadorArquivo.LIMITE+1]) {
            @Override public long getSize() { return 1; }
        };
        assertThrows(MaxUploadSizeExceededException.class, () -> validador.validar(mentiroso));
    }
    @Test void armazenamentoNaoSobrescreveEPermiteRemoverUploadRevertido() throws Exception {
        var local = new ArmazenamentoLocal(pasta.toString());
        local.guardar("projetos/1/prova.pdf", new byte[]{1,2,3}, "application/pdf");
        assertArrayEquals(new byte[]{1,2,3}, local.abrir("projetos/1/prova.pdf").getContentAsByteArray());
        assertThrows(ArmazenamentoIndisponivelException.class, () -> local.guardar("projetos/1/prova.pdf",new byte[]{9},"application/pdf"));
        assertArrayEquals(new byte[]{1,2,3}, local.abrir("projetos/1/prova.pdf").getContentAsByteArray());
        local.remover("projetos/1/prova.pdf");
        assertThrows(RecursoNaoEncontradoException.class, () -> local.abrir("projetos/1/prova.pdf"));
    }
    @Test void bloqueiaCaminhoForaDaRaiz() {
        var local = new ArmazenamentoLocal(pasta.toString());
        assertThrows(RecursoNaoEncontradoException.class, () -> local.guardar("../fora.pdf",new byte[]{1},"application/pdf"));
        assertThrows(RecursoNaoEncontradoException.class, () -> local.abrir("../fora.pdf"));
        assertThrows(RecursoNaoEncontradoException.class, () -> local.remover("../fora.pdf"));
    }
    private byte[] zip(boolean word) throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(bytes)) {
            for (String nome : word ? new String[]{"[Content_Types].xml", "word/document.xml", "_rels/.rels"} : new String[]{"outro.txt"}) {
                zip.putNextEntry(new ZipEntry(nome));
                zip.write("<document/>".getBytes());
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }
}
