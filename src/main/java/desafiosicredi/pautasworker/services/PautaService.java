package desafiosicredi.pautasworker.services;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import desafiosicredi.pautasworker.model.Pauta;
import desafiosicredi.pautasworker.model.StatusPauta;
import desafiosicredi.pautasworker.model.StatusVoto;
import desafiosicredi.pautasworker.repositories.PautaRepository;

@Service
public class PautaService {
    private static final Logger log = LoggerFactory.getLogger(VotoService.class);

    @Autowired
    private PautaRepository pautaRepository;

    @Transactional
    public Pauta verificar(Integer pautaId) throws Exception {
        log.info("Verificando pauta: " + pautaId);

        Pauta pauta = pautaRepository.findById(pautaId).orElse(null);

        if (pauta == null) {
            log.warn("Tentativa de verficar a sessão da pauta " + pautaId + " que não existe.");
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        long pendingVotes = pauta.getVotos().stream().filter(v -> v.getStatus() == StatusVoto.PENDENTE).count();

        if (now.isAfter(pauta.getFim())) {
            pauta.setStatus(StatusPauta.SESSAO_FECHADA);
            log.info("Pauta: " + pautaId + " fechada para votações");
        }
        
        if (pauta.getStatus() == StatusPauta.SESSAO_FECHADA && pendingVotes == 0 ) {
            pauta.setStatus(StatusPauta.CONCLUIDA);
            log.info("Pauta: " + pautaId + " concluída");
        }

        pautaRepository.save(pauta);
        return pauta;
    }
}
