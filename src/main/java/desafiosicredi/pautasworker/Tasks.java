package desafiosicredi.pautasworker;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import com.mysql.cj.log.Log;

import org.springframework.transaction.annotation.Transactional;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
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
import desafiosicredi.pautasworker.services.PautaService;
import desafiosicredi.pautasworker.services.VotoService;


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

    @Autowired
    private VotoService votoService;

    @Autowired
    private PautaService pautaService;

    @RabbitListener(queues = RabbitMQ.FILA_VERIFICAR_STATUS_PAUTA, concurrency = "5")
    public void verificarPauta(Integer pautaId) throws Exception {
        Pauta pauta = pautaService.verificar(pautaId);

        if (pauta.getStatus() != StatusPauta.CONCLUIDA) {
            Thread.sleep(5000);
            rabbitTemplate.convertAndSend(RabbitMQ.FILA_VERIFICAR_STATUS_PAUTA, pauta.getId());
            return;
        }

        StatusPautaDTO statusPauta = StatusPautaDTO.create(pauta);    
        rabbitTemplate.convertAndSend(RabbitMQ.FILA_RESULTADO_PAUTA, statusPauta);
    }

    @RabbitListener(queues = RabbitMQ.FILA_CONTABILIZAR_VOTO, concurrency = "10")
    public void contabilizarVoto(Integer votoId) throws Exception {
        votoService.contabilizar(votoId);
    }

    @RabbitListener(queues = RabbitMQ.FILA_CONTABILIZAR_VOTO_DLQ, concurrency = "2")
    public void descartarVoto(Integer votoId) throws Exception {
        votoService.descartar(votoId);
    }
}
