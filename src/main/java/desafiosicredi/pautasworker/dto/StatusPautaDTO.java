package desafiosicredi.pautasworker.dto;

import java.util.HashMap;
import java.util.Map;

import desafiosicredi.pautasworker.model.Pauta;
import desafiosicredi.pautasworker.model.StatusPauta;
import desafiosicredi.pautasworker.model.StatusVoto;
import desafiosicredi.pautasworker.model.TipoVoto;
import desafiosicredi.pautasworker.model.Voto;

public class StatusPautaDTO {
    private Integer id;

    private String nome;

    private StatusPauta status;

    private Map<TipoVoto, Integer> resultado = new HashMap<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public StatusPauta getStatus() {
        return status;
    }

    public void setStatus(StatusPauta status) {
        this.status = status;
    }

    public Map<TipoVoto, Integer> getResultado() {
        return resultado;
    }

    public void setResultado(Map<TipoVoto, Integer> resultado) {
        this.resultado = resultado;
    }

    public static StatusPautaDTO create(Pauta pauta) {
        StatusPautaDTO dto = new StatusPautaDTO();
        dto.setId(pauta.getId());
        dto.setNome(pauta.getNome());
        dto.setStatus(pauta.getStatus());

        dto.resultado.put(TipoVoto.SIM, 0);
        dto.resultado.put(TipoVoto.NAO, 0);

        for (Voto voto : pauta.getVotos()) {
            if (voto.getStatus() == StatusVoto.CONTABILIZADO) {
                Integer res = dto.resultado.get(voto.getVoto());
                dto.resultado.put(voto.getVoto(), ++res);
            }
        }
        return dto;
    }

    @Override
    public String toString() {
        return "Id=" + id + ", nome=" + nome + ", status=" + status + ", resultado="
                + "SIM (" + resultado.get(TipoVoto.SIM) + "), NAO (" + resultado.get(TipoVoto.NAO) + ")";
    }
}
