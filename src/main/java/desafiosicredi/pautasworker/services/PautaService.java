package desafiosicredi.pautasworker.services;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import com.rabbitmq.client.Return;

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

    /**
     * Verifica uma pauta.
     * 
     * O processo de verificação de uma pauta consiste em validar
     * se o tempo da pauta já esgotou e então encerrá-la
     * ou concluí-la após validar se todos os votos da pauta já foram processados.
     */
    @Transactional
    public Pauta verificar(Integer pautaId) throws Exception {
        log.info("Verificando pauta: " + pautaId);

        Pauta pauta = pautaRepository.findById(pautaId).orElse(null);

        if (pauta == null) {
            log.warn("Tentativa de verficar a sessão da pauta " + pautaId + " que não existe.");
            return null;
        }
        
        // A pauta já está concluída, não há mais nada a fazer.
        if (pauta.getStatus() == StatusPauta.CONCLUIDA) {
            log.warn("Tentativa de verficar a sessão da pauta " + pautaId + " que já está concluída.");
            return null;
        }

        LocalDateTime now = LocalDateTime.now();

        // Total de votos que ainda está pendentes, ou seja, ainda não foram processados.
        long pendingVotes = pauta.getVotos().stream().filter(v -> v.getStatus() == StatusVoto.PENDENTE).count();

        // Se a sessão está aberta mas já expirou seu tempo então encerro a pauta.
        if (pauta.getStatus() == StatusPauta.SESSAO_ABERTA && now.isAfter(pauta.getFim())) {
            pauta.setStatus(StatusPauta.SESSAO_FECHADA);
            log.info("Pauta: " + pautaId + " encerrada para votações");
        }
        
        // Se a pauta está encerrada e todos os seus votos já foram processados
        // então concluo a pauta.
        if (pauta.getStatus() == StatusPauta.SESSAO_FECHADA && pendingVotes == 0 ) {
            pauta.setStatus(StatusPauta.CONCLUIDA);
            log.info("Pauta: " + pautaId + " concluída");
        }

        pautaRepository.save(pauta);
        return pauta;
    }
}
