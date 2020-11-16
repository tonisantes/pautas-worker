package desafiosicredi.pautasworker.repositories;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import desafiosicredi.pautasworker.model.Voto;


public interface VotoRepository extends CrudRepository<Voto, Integer> {
    List<Voto> findByPauta_id(Integer pautaId);
}
