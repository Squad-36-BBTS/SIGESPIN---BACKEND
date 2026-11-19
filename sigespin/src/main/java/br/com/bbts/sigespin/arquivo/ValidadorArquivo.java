package br.com.bbts.sigespin.arquivo;

import br.com.bbts.sigespin.exception.ArquivoInvalidoException;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Locale;
import java.util.zip.ZipFile;

@Component
public class ValidadorArquivo {
    public static final long LIMITE = 10L * 1024 * 1024;
    public record Validado(String nome, String extensao, String contentType, byte[] conteudo) { }

    public Validado validar(MultipartFile arquivo) throws IOException {
        if (arquivo.isEmpty()) throw new ArquivoInvalidoException("O arquivo está vazio.");
        if (arquivo.getSize() > LIMITE) throw new MaxUploadSizeExceededException(LIMITE);
        String original = arquivo.getOriginalFilename();
        if (original == null || original.isBlank()) throw new ArquivoInvalidoException("Informe o nome do arquivo.");
        String nome = original.replace('\\', '/');
        nome = nome.substring(nome.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "_").strip();
        if (nome.isBlank() || nome.length() > 255) throw new ArquivoInvalidoException("Nome de arquivo inválido ou maior que 255 caracteres.");
        String extensao = nome.substring(nome.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        byte[] bytes;
        try (InputStream entrada = arquivo.getInputStream()) { bytes = entrada.readNBytes((int) LIMITE + 1); }
        if (bytes.length > LIMITE) throw new MaxUploadSizeExceededException(LIMITE);
        String tipo = switch (extensao) {
            case "jpg", "jpeg" -> validarJpeg(bytes);
            case "pdf" -> validarPdf(bytes);
            case "doc" -> validarDoc(bytes);
            case "docx" -> validarDocx(bytes);
            default -> throw new ArquivoInvalidoException("Envie JPEG, PDF ou Word (DOC/DOCX), até 10 MB por arquivo.");
        };
        return new Validado(nome, extensao, tipo, bytes);
    }

    private String validarJpeg(byte[] bytes) {
        if (bytes.length < 4 || bytes[0] != (byte) 0xff || bytes[1] != (byte) 0xd8 || bytes[2] != (byte) 0xff) {
            throw new ArquivoInvalidoException("O conteúdo não corresponde a uma imagem JPEG.");
        }
        return "image/jpeg";
    }

    private String validarPdf(byte[] bytes) {
        if (bytes.length < 5 || !new String(bytes, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-")) {
            throw new ArquivoInvalidoException("O conteúdo não corresponde a um PDF.");
        }
        return "application/pdf";
    }

    private String validarDoc(byte[] bytes) {
        try (var ole = new POIFSFileSystem(new ByteArrayInputStream(bytes))) {
            if (!ole.getRoot().hasEntry("WordDocument")) throw new IOException("Documento Word ausente.");
            return "application/msword";
        } catch (IOException | RuntimeException e) {
            throw new ArquivoInvalidoException("O conteúdo não corresponde a um documento DOC.");
        }
    }

    private String validarDocx(byte[] bytes) throws IOException {
        Path temporario = Files.createTempFile("sigespin-docx-", ".zip");
        try {
            Files.write(temporario, bytes);
            try (ZipFile zip = new ZipFile(temporario.toFile())) {
                if (zip.getEntry("[Content_Types].xml") == null || zip.getEntry("word/document.xml") == null
                        || zip.getEntry("_rels/.rels") == null) {
                    throw new IOException("Estrutura Word ausente.");
                }
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            } catch (IOException e) { throw new ArquivoInvalidoException("O conteúdo não corresponde a um documento DOCX."); }
        } finally { Files.deleteIfExists(temporario); }
    }
}
