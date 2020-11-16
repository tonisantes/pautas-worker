package desafiosicredi.pautasworker;

import java.time.LocalDateTime;
import java.util.List;

import javax.transaction.Transactional;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

    @Transactional
    @RabbitListener(queues = RabbitMQ.FILA_VERIFICAR_STATUS_PAUTA, concurrency = "5")
    public void verificarStatusSessao(Integer pautaId) throws Exception {
        log.info("Verificando sessao: " + pautaId);

        Pauta pauta = pautaRepository.findById(pautaId).orElse(null);

        if (pauta == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        if (now.isEqual(pauta.getFim()) || now.isAfter(pauta.getFim())) {
            pauta.setStatus(StatusPauta.SESSAO_FINALIZADA);
            pautaRepository.save(pauta);
        }

        StatusPautaDTO statusPauta = StatusPautaDTO.create(pauta);
        
        log.info(statusPauta.toString());

        rabbitTemplate.convertAndSend(RabbitMQ.FILA_STATUS_PAUTA, statusPauta);

        Thread.sleep(5000);
        rabbitTemplate.convertAndSend(RabbitMQ.FILA_VERIFICAR_STATUS_PAUTA, pauta.getId());
    }

    @Transactional
    @RabbitListener(queues = RabbitMQ.FILA_CONTABILIZAR_VOTO, concurrency = "5")
    public void contabilizarVoto(Integer votoId) throws Exception {
        System.out.println("Contabilizando voto: " + votoId);


        Voto voto = votoRepository.findById(votoId).orElse(null);

        if (voto == null || voto.getStatus() != StatusVoto.PENDENTE) {
            return;
        }

        voto.setStatus(StatusVoto.CONTABILIZADO);
        votoRepository.save(voto);
    }
}
