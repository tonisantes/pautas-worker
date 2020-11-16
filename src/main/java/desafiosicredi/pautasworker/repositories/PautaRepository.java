package desafiosicredi.pautasworker.repositories;

import org.springframework.data.repository.CrudRepository;

import desafiosicredi.pautasworker.model.Pauta;


public interface PautaRepository extends CrudRepository<Pauta, Integer> {
    
}
