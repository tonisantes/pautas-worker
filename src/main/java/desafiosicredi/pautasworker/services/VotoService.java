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
    
    @Transactional
    public void contabilizar(Integer votoId) throws Exception {
        log.info("Contabilizando voto: " + votoId);

        Voto voto = votoRepository.findById(votoId).orElse(null);

        if (voto == null) {
            log.warn("Tentativa de contabilizar o voto " + votoId + " que não existe.");
            return;
        }

        if (voto.getStatus() != StatusVoto.PENDENTE) {
            log.warn("Tentativa de contabilizar o voto " + votoId + " que está com status diferente de PENDENTE. Ignorando.");
            return;
        }

        try {
            // Map response = restTemplate.getForObject("https://user-info.herokuapp.com/users/" + voto.getCpfAssociado(), Map.class);
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
