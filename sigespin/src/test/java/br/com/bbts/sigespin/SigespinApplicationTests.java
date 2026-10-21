package br.com.bbts.sigespin;

import br.com.bbts.sigespin.model.entity.*;
import br.com.bbts.sigespin.model.enums.*;
import br.com.bbts.sigespin.repository.*;
import jakarta.persistence.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
 "spring.datasource.url=${SIGESPIN_TEST_DB_URL}",
 "spring.datasource.username=${SIGESPIN_TEST_DB_USER:postgres}",
 "spring.datasource.password=${SIGESPIN_TEST_DB_PASSWORD:}",
 "spring.jpa.hibernate.ddl-auto=validate"})
@EnabledIfEnvironmentVariable(named="SIGESPIN_TEST_DB_URL", matches=".+")
class SigespinApplicationTests {
 @Autowired EntityManagerFactory emf;
 @Autowired ProjetoRepository projetos;
 @Autowired EtapaKanbanRepository etapas;
 @Test void validaMapeamentoConsultasGlobaisEConcorrenciaOtimista() {
  Long projetoId=null, usuarioId=null;
  try(var setup=emf.createEntityManager()) {
   setup.getTransaction().begin();
   Usuario u=new Usuario(); u.setNome("Teste"); u.setEmail(java.util.UUID.randomUUID()+"@teste");
   u.setSenhaHash("hash"); u.setPerfil(PerfilUsuario.ANALISTA); u.setAtivo(true); setup.persist(u);
   Projeto p=new Projeto(); p.setTitulo("Portfolio global"); p.setDescricaoDor("Dor"); p.setAreaDemandanteId(1L);
   p.setResponsavelPo(u); p.setArena(ArenaTipo.A_DEFINIR); p.setTrlAtual(5);
   p.setEtapa(setup.find(EtapaKanban.class,etapas.findByAtivaTrueOrderByOrdemAscIdAsc().getFirst().getId()));
   setup.persist(p); setup.getTransaction().commit(); projetoId=p.getId(); usuarioId=u.getId();
   assertEquals(1,projetos.findAllWithFilters(null,null,"Portfolio global",PageRequest.of(0,10)).getTotalElements());
   assertEquals(1,projetos.findAllWithFilters(ArenaTipo.A_DEFINIR,p.getEtapa().getId(),null,PageRequest.of(0,10)).getTotalElements());
   try(var a=emf.createEntityManager();var b=emf.createEntityManager()) {
    a.getTransaction().begin(); b.getTransaction().begin();
    Projeto pa=a.find(Projeto.class,projetoId), pb=b.find(Projeto.class,projetoId);
    pa.setTrlAtual(3); a.getTransaction().commit();
    pb.setTrlAtual(4);
    assertThrows(RollbackException.class,()->b.getTransaction().commit());
   }
   assertEquals(3,projetos.findById(projetoId).orElseThrow().getTrlAtual());
  } finally {
   if(projetoId!=null) projetos.deleteById(projetoId);
   if(usuarioId!=null) try(var cleanup=emf.createEntityManager()) {
    cleanup.getTransaction().begin(); cleanup.remove(cleanup.find(Usuario.class,usuarioId)); cleanup.getTransaction().commit();
   }
  }
 }
}
