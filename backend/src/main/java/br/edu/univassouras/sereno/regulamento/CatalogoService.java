package br.edu.univassouras.sereno.regulamento;

import br.edu.univassouras.sereno.calculo.RegrasCalculo;
import br.edu.univassouras.sereno.comum.ErroDeNegocio;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** API pública do módulo: leitura do catálogo e conversão para as regras do motor de cálculo. */
@Service
@Transactional(readOnly = true)
public class CatalogoService {

    private final RegulamentoRepository regulamentos;

    CatalogoService(RegulamentoRepository regulamentos) {
        this.regulamentos = regulamentos;
    }

    public Optional<Catalogo> vigente() {
        return regulamentos.findVigente().map(CatalogoService::paraCatalogo);
    }

    public Optional<RegrasCalculo> regrasVigentes() {
        return regulamentos.findVigente().map(CatalogoService::paraRegras);
    }

    public Optional<UUID> idVigente() {
        return regulamentos.findVigente().map(Regulamento::getId);
    }

    public Optional<Catalogo> catalogo(UUID regulamentoId) {
        return regulamentos.findById(regulamentoId).map(CatalogoService::paraCatalogo);
    }

    public Optional<RegrasCalculo> regras(UUID regulamentoId) {
        return regulamentos.findById(regulamentoId).map(CatalogoService::paraRegras);
    }

    /** Troca as palavras-chave e a origem esperada de um item do regulamento vigente. */
    @Transactional
    public Catalogo.ItemDto ajustarClassificacao(int numero, List<String> palavrasChave, OrigemEsperada origem) {
        Regulamento vigente = regulamentos.findVigente()
                .orElseThrow(() -> ErroDeNegocio.conflito("sem-regulamento", "Nenhum regulamento publicado."));
        ItemAtividade item = vigente.getMacroatividades().stream()
                .flatMap(m -> m.getItens().stream())
                .filter(i -> i.getNumero() == numero)
                .findFirst()
                .orElseThrow(() -> ErroDeNegocio.naoEncontrado("Item " + numero + " não existe no regulamento vigente."));
        item.ajustarClassificacao(palavrasChave, origem);
        return paraCatalogo(vigente).item(numero).orElseThrow().item();
    }

    /** Define (ou apaga, com nulo) o total de horas exigidas do regulamento vigente. */
    @Transactional
    public Catalogo definirHorasExigidas(java.math.BigDecimal horas) {
        Regulamento vigente = regulamentos.findVigente()
                .orElseThrow(() -> ErroDeNegocio.conflito("sem-regulamento", "Nenhum regulamento publicado."));
        vigente.definirHorasExigidas(horas);
        return paraCatalogo(vigente);
    }

    private static Catalogo paraCatalogo(Regulamento r) {
        return new Catalogo(r.getId(), r.getNome(), r.getVersao(), r.getHorasExigidas().orElse(null),
                r.getMacroatividades().stream()
                        .map(m -> new Catalogo.MacroDto(m.getCodigo(), m.getOrdem(), m.getNome(), m.getLimiteHoras().orElse(null),
                                m.getItens().stream()
                                        .filter(ItemAtividade::isAtivo)
                                        .map(i -> new Catalogo.ItemDto(i.getNumero(), i.getNome(), i.getDescricao().orElse(null),
                                                i.getHorasPorOcorrencia(), i.getEstrategia(), i.getTetoPorOcorrencia().orElse(null),
                                                i.getLimiteOcorrencias().orElse(null), i.getLimiteHoras().orElse(null),
                                                i.getPalavrasChave(), i.getOrigemEsperada()))
                                        .toList()))
                        .toList());
    }

    private static RegrasCalculo paraRegras(Regulamento r) {
        Map<String, RegrasCalculo.Macro> macros = r.getMacroatividades().stream()
                .map(m -> new RegrasCalculo.Macro(m.getCodigo(), m.getLimiteHoras()))
                .collect(Collectors.toMap(RegrasCalculo.Macro::codigo, Function.identity()));
        Map<Integer, RegrasCalculo.Item> itens = r.getMacroatividades().stream()
                .flatMap(m -> m.getItens().stream().map(i -> new RegrasCalculo.Item(i.getNumero(), m.getCodigo(),
                        i.getHorasPorOcorrencia(), i.getEstrategia(), i.getTetoPorOcorrencia(), i.getLimiteOcorrencias(),
                        i.getLimiteHoras())))
                .collect(Collectors.toMap(RegrasCalculo.Item::numero, Function.identity()));
        return new RegrasCalculo(r.getHorasExigidas(), itens, macros);
    }
}
