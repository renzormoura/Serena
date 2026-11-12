package br.edu.univassouras.sereno.certificados.armazenamento;

import org.springframework.core.io.Resource;

/**
 * Onde os PDFs e imagens ficam guardados. Hoje em disco local; a implementação S3 (MinIO/R2) entra no deploy
 * de produção sem mudar quem usa esta interface.
 */
public interface ArmazenamentoArquivos {

    /** Guarda o conteúdo e devolve a chave gerada (nunca derivada do nome enviado pelo usuário). */
    String guardar(byte[] conteudo, TipoArquivo tipo);

    Resource abrir(String chave);
}
