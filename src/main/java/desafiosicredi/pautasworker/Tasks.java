package desafiosicredi.pautasworker;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import javax.transaction.Transactional;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import desafiosicredi.pautasworker.config.RabbitMQ;
import desafiosicredi.pautasworker.dto.StatusPautaDTO;
import desafiosicredi.pautasworker.model.Pauta;
import desafiosicredi.pautasworker.model.StatusPauta;
import desafiosicredi.pautasworker.model.StatusVoto;
import desafiosicredi.pautasworker.model.Voto;
import desafiosicredi.pautasworker.repositories.PautaRepository;
import desafiosicredi.pautasworker.repositories.VotoRepository;


@Component
public class Tasks {

    private static final Logger log = LoggerFactory.getLogger(Tasks.class);

    @Autowired
    private PautaRepository pautaRepository;

    @Autowired
    private VotoRepository votoRepository;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RestTemplate restTemplate;

    @Transactional
    @RabbitListener(queues = RabbitMQ.FILA_VERIFICAR_STATUS_PAUTA, concurrency = "5")
    public void verificarStatusSessao(Integer pautaId) throws Exception {
        log.info("Verificando sessao: " + pautaId);

        Pauta pauta = pautaRepository.findById(pautaId).orElse(null);

        if (pauta == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        long pendingVotes = pauta.getVotos().stream().filter(v -> v.getStatus() == StatusVoto.PENDENTE).count();

        if (now.isEqual(pauta.getFim()) || now.isAfter(pauta.getFim())) {
            pauta.setStatus(StatusPauta.SESSAO_FECHADA);
        }
        
        if (pauta.getStatus() == StatusPauta.SESSAO_FECHADA) {
            if (pendingVotes == 0 || pauta.getFim().until(now, ChronoUnit.HOURS) > 2) {
                pauta.setStatus(StatusPauta.CONCLUIDA);
            }
        }

        pautaRepository.save(pauta);

        StatusPautaDTO statusPauta = StatusPautaDTO.create(pauta);
        
        log.info(statusPauta.toString());

        rabbitTemplate.convertAndSend(RabbitMQ.FILA_STATUS_PAUTA, statusPauta);

        if (pauta.getStatus() != StatusPauta.CONCLUIDA) {
            Thread.sleep(5000);
            rabbitTemplate.convertAndSend(RabbitMQ.FILA_VERIFICAR_STATUS_PAUTA, pauta.getId());
        }
    }

    @Transactional
    @RabbitListener(queues = RabbitMQ.FILA_CONTABILIZAR_VOTO, concurrency = "5")
    public void contabilizarVoto(Integer votoId) throws Exception {
        log.info("Contabilizando voto: " + votoId);

        Voto voto = votoRepository.findById(votoId).orElse(null);

        if (voto == null || voto.getStatus() != StatusVoto.PENDENTE) {
            return;
        }

        try {
            Map response = restTemplate.getForObject("https://user-info.herokuapp.com/users/" + voto.getCpfAssociado(), Map.class);
            if (response.containsKey("status") && response.get("status") == "ABLE_TO_VOTE") {
                voto.setStatus(StatusVoto.CONTABILIZADO);
            }
            else {
                voto.setStatus(StatusVoto.REJEITADO);
            }
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() != HttpStatus.NOT_FOUND)
                throw ex;
                
            voto.setStatus(StatusVoto.REJEITADO);
        }

        votoRepository.save(voto);
    }

    @Transactional
    @RabbitListener(queues = RabbitMQ.FILA_CONTABILIZAR_VOTO_DLQ, concurrency = "2")
    public void descartarVoto(Integer votoId) throws Exception {
        log.info("Decartando voto: " + votoId);

        Voto voto = votoRepository.findById(votoId).orElse(null);

        if (voto == null) {
            return;
        }

        voto.setStatus(StatusVoto.ERRO);
    }
}
