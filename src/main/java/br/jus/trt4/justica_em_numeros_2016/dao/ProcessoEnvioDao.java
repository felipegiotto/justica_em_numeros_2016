package br.jus.trt4.justica_em_numeros_2016.dao;

import java.time.LocalDate;
import java.util.List;

import javax.persistence.TypedQuery;

import br.jus.trt4.justica_em_numeros_2016.entidades.ProcessoEnvio;
import br.jus.trt4.justica_em_numeros_2016.enums.OrigemProcessoEnum;
import br.jus.trt4.justica_em_numeros_2016.enums.TipoRemessaEnum;

/**
 * Classe responsável por acessar dados da entidade ProcessoEnvio
 * 
 * @author ivan.franca@trt6.jus.br
 */
public class ProcessoEnvioDao extends DataJudBaseDao<ProcessoEnvio> {

	/**
	 * Recupera os processos que serão enviados em uma Remessa por grau e origem.
	 * 
	 * @param dataCorte data de corte
	 * @param tipoRemessa tipo da Remessa
	 * @param grau grau do processo
	 * @param origens origens dos processos
	 * 
	 * @return lista de processos
	 */
	public List<ProcessoEnvio> getProcessosRemessa(LocalDate dataCorte,	TipoRemessaEnum tipoRemessa, String grau, List<OrigemProcessoEnum> origens) {
		StringBuilder hql = new StringBuilder();
		hql.append("select pe from ProcessoEnvio pe ");
		hql.append(" where pe.remessa.dataCorte = :dataCorte ");
		hql.append(" and pe.remessa.tipoRemessa = :tipoRemessa ");
		hql.append(" and pe.grau = :grau ");
		hql.append(" and pe.origem IN ( :origens ) ");
		
		TypedQuery<ProcessoEnvio> query = JPAUtil.getEntityManager().createQuery(hql.toString(), ProcessoEnvio.class);
		query.setParameter("dataCorte", dataCorte);
		query.setParameter("tipoRemessa", tipoRemessa);
		query.setParameter("grau", grau);
		query.setParameter("origens", origens);
		return query.getResultList();
	}
	
	/**
	 * Recupera os processos que serão enviados em uma Remessa.
	 * 
	 * @param dataCorte data de corte
	 * @param tipoRemessa tipo da Remessa
	 * 
	 * @return lista de processos
	 */
	public List<ProcessoEnvio> getProcessosRemessa(LocalDate dataCorte,	TipoRemessaEnum tipoRemessa) {
		StringBuilder hql = new StringBuilder();
		hql.append("select pe from ProcessoEnvio pe ");
		hql.append(" where pe.remessa.dataCorte = :dataCorte ");
		hql.append(" and pe.remessa.tipoRemessa = :tipoRemessa ");

		TypedQuery<ProcessoEnvio> query = JPAUtil.getEntityManager().createQuery(hql.toString(), ProcessoEnvio.class);
		query.setParameter("dataCorte", dataCorte);
		query.setParameter("tipoRemessa", tipoRemessa);
		
		return query.getResultList();
	}
	

	public Long getQuantidadeProcessosRemessa(LocalDate dataCorte,	TipoRemessaEnum tipoRemessa, List<String> graus) {
		StringBuilder hql = new StringBuilder();
		hql.append("select count(pe) from ProcessoEnvio pe ");
		hql.append(" where pe.remessa.dataCorte = :dataCorte ");
		hql.append(" and pe.remessa.tipoRemessa = :tipoRemessa ");
		if (graus != null) {
			hql.append(" and pe.grau IN ( :graus ) ");			
		}

		TypedQuery<Long> query = JPAUtil.getEntityManager().createQuery(hql.toString(), Long.class);
		query.setParameter("dataCorte", dataCorte);
		query.setParameter("tipoRemessa", tipoRemessa);
		if (graus != null) {
			query.setParameter("graus", graus);
		}

		return getSingleResultOrNull(query);
	}

}
