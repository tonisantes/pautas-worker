package desafiosicredi.pautasworker;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import desafiosicredi.pautasworker.config.RabbitMQConfig;
import desafiosicredi.pautasworker.dto.StatusPautaDTO;
import desafiosicredi.pautasworker.model.Pauta;
import desafiosicredi.pautasworker.model.StatusPauta;
import desafiosicredi.pautasworker.services.PautaService;
import desafiosicredi.pautasworker.services.VotoService;


@Component
public class Tasks {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private VotoService votoService;

    @Autowired
    private PautaService pautaService;

    /**
     * Consumidor que verifica de tempos em tesmpos o status de uma pauta.
     * Essa verficação consiste em alterar o status da pauta e concluía quando todos os votos
     * tiverem sido processados.
     */
    @RabbitListener(queues = RabbitMQConfig.FILA_VERIFICAR_STATUS_PAUTA, concurrency = "5")
    public void verificarPauta(Integer pautaId) throws Exception {

        // Delega para o Service fazer a verificação (ver os comentários do Service para mais detalhes).
        Pauta pauta = pautaService.verificar(pautaId);

        // Caso a pauta não exista apenas retorna.
        if (pauta == null) {
            return;
        }
        
        /*
         Caso a pauta ainda não esteja concluída aguardo 5 segundos e reenfilero
         a mesma mensagem para nova verificação.
        */
        if (pauta.getStatus() != StatusPauta.CONCLUIDA) {
            Thread.sleep(5000); // 5 Segundos
            rabbitTemplate.convertAndSend(RabbitMQConfig.FILA_VERIFICAR_STATUS_PAUTA, pauta.getId());
            return;
        }

        /*
        Se a pauta está concluída envio o resultado para outra fila.
        */
        StatusPautaDTO statusPauta = StatusPautaDTO.create(pauta);    
        rabbitTemplate.convertAndSend(RabbitMQConfig.FILA_RESULTADO_PAUTA, statusPauta);
    }

    /**
     * Consumidor para processar um voto.
     */
    @RabbitListener(queues = RabbitMQConfig.FILA_CONTABILIZAR_VOTO, concurrency = "10")
    public void contabilizarVoto(Integer votoId) throws Exception {

        // Delego para o Service processar o voto (ver os comentários do Service para mais detalhes)
        votoService.contabilizar(votoId);
    }

    /**
     * Caso haja erros inesperados no processamento de um voto
     * mesmo depois de todas as retentivas, então o voto cai nesse consumidor
     * que na pratica vai descartar/desistir de processar o voto.
     */
    @RabbitListener(queues = RabbitMQConfig.FILA_CONTABILIZAR_VOTO_DLQ, concurrency = "2")
    public void descartarVoto(Integer votoId) throws Exception {
        votoService.descartar(votoId);
    }
}
