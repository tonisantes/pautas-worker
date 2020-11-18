package desafiosicredi.pautasworker.services;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import desafiosicredi.pautasworker.model.StatusVoto;
import desafiosicredi.pautasworker.model.Voto;
import desafiosicredi.pautasworker.repositories.VotoRepository;

@Service
public class VotoService {

    private static final Logger log = LoggerFactory.getLogger(VotoService.class);

    @Autowired
    private VotoRepository votoRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private Environment env;
    
    /**
     * Tenta contabilizar um voto.
     * 
     * A parte mais critica desse processamento consiste em se integrar
     * com o serviço externo de validação do CPF, no mais apenas rejeito ou contabilizo o voto.
     */
    @Transactional
    public void contabilizar(Integer votoId) throws Exception {
        log.info("Contabilizando voto: " + votoId);

        Voto voto = votoRepository.findById(votoId).orElse(null);

        if (voto == null) {
            log.warn("Tentativa de contabilizar o voto " + votoId + " que não existe.");
            return;
        }
        
        // O voto já foi contabilizado?
        if (voto.getStatus() != StatusVoto.PENDENTE) {
            log.warn("Tentativa de contabilizar o voto " + votoId + " que está com status diferente de PENDENTE. Ignorando.");
            return;
        }

        try {
            /*
             Faço a integração com o serviço externo de validação de cpf.
             Caso o cpf seja apto para votar altero o status do voto para `CONTABILIZADO`,
             caso o cpf seja inválido ou inapto para votar, altero o status para `REJEITADO`.

             OBS.:
             Todos os cpfs válidos que testei voltaram como `UNABLE_TO_VOTE`.
            */
            Map response = restTemplate.getForObject(env.getProperty("validadorcpf.url") + voto.getCpfAssociado(), Map.class);
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
        log.info("Voto " + voto.getId() + ": " + voto.getStatus());
    }

    /**
     * Apenas altero o status do voto para `ERRO`, significando
     * que o voto não pode ser processado.
     */
    @Transactional
    public void descartar(Integer votoId) throws Exception {
        log.info("Decartando voto: " + votoId);

        Voto voto = votoRepository.findById(votoId).orElse(null);

        if (voto == null) {
            log.warn("Tentativa de descartar o voto " + votoId + " que não existe.");
            return;
        }

        voto.setStatus(StatusVoto.ERRO);
    }
}
