package desafiosicredi.pautasworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.h2.util.Task;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;

import desafiosicredi.pautasworker.model.Pauta;
import desafiosicredi.pautasworker.model.StatusPauta;
import desafiosicredi.pautasworker.model.StatusVoto;
import desafiosicredi.pautasworker.model.TipoVoto;
import desafiosicredi.pautasworker.model.Voto;
import desafiosicredi.pautasworker.repositories.PautaRepository;
import desafiosicredi.pautasworker.repositories.VotoRepository;
import desafiosicredi.pautasworker.services.PautaService;
import desafiosicredi.pautasworker.services.VotoService;

@SpringBootTest
class PautasWorkerApplicationTests {

	@MockBean
	private PautaRepository pautaRepository;

	@MockBean
	private VotoRepository votoRepository;

	@MockBean
	private RestTemplate restTemplate;
	
	@MockBean
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private Environment env;
	
	@Autowired
	private VotoService votoService;

	@Autowired
	private PautaService pautaService;

	@Test
	void contextLoads() {
	}

	@Test
	public void contabilizarVoto() throws Exception {
		Voto voto = new Voto();
		voto.setId(1);
		voto.setVoto(TipoVoto.SIM);
		voto.setCpfAssociado("07290790901");
		voto.setStatus(StatusVoto.PENDENTE);

		when(votoRepository.findById(1)).thenReturn(Optional.of(voto));
		
		when(restTemplate.getForObject(env.getProperty("validadorcpf.url") + voto.getCpfAssociado(), Map.class)).thenReturn(
			new HashMap<String, String>(){{
				put("status", "ABLE_TO_VOTE");
			}}
		);

		votoService.contabilizar(voto.getId());
		assertThat(voto.getStatus()).isEqualTo(StatusVoto.CONTABILIZADO);
	}

	@Test
	public void contabilizarVotoNaoPendente() throws Exception {
		Voto voto = new Voto();
		voto.setId(1);
		voto.setVoto(TipoVoto.SIM);
		voto.setCpfAssociado("07290790901");
		voto.setStatus(StatusVoto.REJEITADO);

		when(votoRepository.findById(1)).thenReturn(Optional.of(voto));

		votoService.contabilizar(voto.getId());
		assertThat(voto.getStatus()).isEqualTo(StatusVoto.REJEITADO);
	}

	@Test
	public void contabilizarVotoRejeitado() throws Exception {
		Voto voto = new Voto();
		voto.setId(1);
		voto.setVoto(TipoVoto.SIM);
		voto.setCpfAssociado("07290790901");
		voto.setStatus(StatusVoto.PENDENTE);

		when(votoRepository.findById(1)).thenReturn(Optional.of(voto));
		
		when(restTemplate.getForObject(env.getProperty("validadorcpf.url") + voto.getCpfAssociado(), Map.class)).thenReturn(
			new HashMap<String, String>(){{
				put("status", "UNABLE_TO_VOTE");
			}}
		);

		votoService.contabilizar(voto.getId());
		assertThat(voto.getStatus()).isEqualTo(StatusVoto.REJEITADO);
	}

	@Test
	public void descartarVoto() throws Exception {
		Voto voto = new Voto();
		voto.setId(1);
		voto.setVoto(TipoVoto.SIM);
		voto.setCpfAssociado("07290790901");
		voto.setStatus(StatusVoto.PENDENTE);

		when(votoRepository.findById(1)).thenReturn(Optional.of(voto));

		votoService.descartar(voto.getId());
		assertThat(voto.getStatus()).isEqualTo(StatusVoto.ERRO);
	}

	@Test
	public void verificarSessaoEmAndamento() throws Exception {
		Pauta pauta = new Pauta();
		pauta.setId(1);
		pauta.setNome("teste");
		pauta.setStatus(StatusPauta.SESSAO_ABERTA);
		pauta.setInicio(LocalDateTime.now().minusSeconds(60));
		pauta.setFim(pauta.getInicio().plusSeconds(61));

		Voto voto1 = new Voto();
		voto1.setId(1);
		voto1.setVoto(TipoVoto.SIM);
		voto1.setCpfAssociado("07290790901");
		voto1.setStatus(StatusVoto.CONTABILIZADO);

		Voto voto2 = new Voto();
		voto1.setId(1);
		voto1.setVoto(TipoVoto.SIM);
		voto1.setCpfAssociado("07290790902");
		voto1.setStatus(StatusVoto.CONTABILIZADO);

		List<Voto> votos = new ArrayList<>();
		votos.add(voto1);
		votos.add(voto2);

		pauta.setVotos(votos);
		
		when(pautaRepository.findById(1)).thenReturn(Optional.of(pauta));

		pautaService.verificar(pauta.getId());
		assertThat(pauta.getStatus()).isEqualTo(StatusPauta.SESSAO_ABERTA);
	}

	@Test
	public void verificarEncerramentoPauta() throws Exception {
		Pauta pauta = new Pauta();
		pauta.setId(1);
		pauta.setNome("teste");
		pauta.setStatus(StatusPauta.SESSAO_ABERTA);
		pauta.setInicio(LocalDateTime.now().minusSeconds(60));
		pauta.setFim(pauta.getInicio().plusSeconds(59));

		Voto voto1 = new Voto();
		voto1.setId(1);
		voto1.setVoto(TipoVoto.SIM);
		voto1.setCpfAssociado("07290790901");
		voto1.setStatus(StatusVoto.CONTABILIZADO);

		Voto voto2 = new Voto();
		voto1.setId(1);
		voto1.setVoto(TipoVoto.SIM);
		voto1.setCpfAssociado("07290790902");
		voto1.setStatus(StatusVoto.PENDENTE);

		List<Voto> votos = new ArrayList<>();
		votos.add(voto1);
		votos.add(voto2);

		pauta.setVotos(votos);
		
		when(pautaRepository.findById(1)).thenReturn(Optional.of(pauta));

		pautaService.verificar(pauta.getId());
		assertThat(pauta.getStatus()).isEqualTo(StatusPauta.SESSAO_FECHADA);
	}

	@Test
	public void verificarConclusaoPauta() throws Exception {
		Pauta pauta = new Pauta();
		pauta.setId(1);
		pauta.setNome("teste");
		pauta.setStatus(StatusPauta.SESSAO_ABERTA);
		pauta.setInicio(LocalDateTime.now().minusSeconds(60));
		pauta.setFim(pauta.getInicio().plusSeconds(40));

		Voto voto1 = new Voto();
		voto1.setId(1);
		voto1.setVoto(TipoVoto.SIM);
		voto1.setCpfAssociado("07290790901");
		voto1.setStatus(StatusVoto.REJEITADO);

		Voto voto2 = new Voto();
		voto2.setId(1);
		voto2.setVoto(TipoVoto.SIM);
		voto2.setCpfAssociado("07290790902");
		voto2.setStatus(StatusVoto.CONTABILIZADO);

		List<Voto> votos = new ArrayList<>();
		votos.add(voto1);
		votos.add(voto2);

		pauta.setVotos(votos);
		
		when(pautaRepository.findById(1)).thenReturn(Optional.of(pauta));

		pautaService.verificar(pauta.getId());
		assertThat(pauta.getStatus()).isEqualTo(StatusPauta.CONCLUIDA);
	}

}
