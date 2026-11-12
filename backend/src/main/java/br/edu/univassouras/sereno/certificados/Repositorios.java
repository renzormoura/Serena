package br.edu.univassouras.sereno.certificados;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

interface CertificadoRepository extends Repository<Certificado, UUID> {

    @EntityGraph(attributePaths = "arquivo")
    @Query("select c from Certificado c where c.alunoId = :alunoId and c.excluidoEm is null order by c.criadoEm desc")
    List<Certificado> ativosDoAluno(UUID alunoId);

    @EntityGraph(attributePaths = "arquivo")
    Optional<Certificado> findById(UUID id);

    @Query("""
            select count(c) > 0 from Certificado c
            where c.alunoId = :alunoId and c.excluidoEm is null and c.arquivo.sha256 = :sha256""")
    boolean existeArquivoAtivo(UUID alunoId, String sha256);

    Certificado save(Certificado certificado);

    /** Hashes, dentre os informados, que estão ativos em mais de uma conta de aluno. */
    @Query("""
            select c.arquivo.sha256 from Certificado c
            where c.excluidoEm is null and c.arquivo.sha256 in :hashes
            group by c.arquivo.sha256
            having count(distinct c.alunoId) > 1""")
    List<String> arquivosEmMaisDeUmaConta(java.util.Collection<String> hashes);

    @EntityGraph(attributePaths = "arquivo")
    @Query("select c from Certificado c where c.alunoId in :alunos and c.excluidoEm is null")
    List<Certificado> ativosDosAlunos(java.util.Collection<UUID> alunos);

    record SugeridoConfirmado(int sugerido, int confirmado, long total) {
    }

    /** Quantas vezes cada item sugerido virou cada item confirmado, só entre certificados já conferidos. */
    @Query("""
            select new br.edu.univassouras.sereno.certificados.CertificadoRepository$SugeridoConfirmado(
                       c.itemSugerido, c.itemNumero, count(c))
            from Certificado c
            where c.itemSugerido is not null and c.itemNumero is not null and c.excluidoEm is null and c.status in :status
            group by c.itemSugerido, c.itemNumero""")
    List<SugeridoConfirmado> sugeridoContraConfirmado(java.util.Collection<StatusCertificado> status);
}

interface ArquivoRepository extends Repository<Arquivo, UUID> {

    Arquivo save(Arquivo arquivo);
}

interface AvaliacaoRepository extends Repository<Avaliacao, UUID> {

    Avaliacao save(Avaliacao avaliacao);

    List<Avaliacao> findByCertificadoIdOrderByCriadoEmDesc(UUID certificadoId);
}
