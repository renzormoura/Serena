package br.edu.univassouras.sereno.certificados.armazenamento;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.YearMonth;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/** Arquivos em disco, organizados por ano/mês, com nomes aleatórios. */
@Component
class ArmazenamentoLocal implements ArmazenamentoArquivos {

    private final Path raiz;

    ArmazenamentoLocal(@Value("${sereno.arquivos.diretorio:./dados/arquivos}") Path raiz) throws IOException {
        this.raiz = Files.createDirectories(raiz).toRealPath();
    }

    @Override
    public String guardar(byte[] conteudo, TipoArquivo tipo) {
        YearMonth mes = YearMonth.now();
        String chave = "%d/%02d/%s.%s".formatted(mes.getYear(), mes.getMonthValue(), UUID.randomUUID(), tipo.extensao());
        Path destino = resolver(chave);
        try {
            Files.createDirectories(destino.getParent());
            Files.write(destino, conteudo, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível guardar o arquivo", e);
        }
        return chave;
    }

    @Override
    public Resource abrir(String chave) {
        return new FileSystemResource(resolver(chave));
    }

    private Path resolver(String chave) {
        Path caminho = raiz.resolve(chave).normalize();
        if (!caminho.startsWith(raiz)) {
            throw new IllegalArgumentException("Chave de arquivo inválida");
        }
        return caminho;
    }
}
