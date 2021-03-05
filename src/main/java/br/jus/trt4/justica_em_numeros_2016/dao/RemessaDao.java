package br.jus.trt4.justica_em_numeros_2016.dao;

import java.time.LocalDate;

import javax.persistence.TypedQuery;

import br.jus.trt4.justica_em_numeros_2016.entidades.Remessa;
import br.jus.trt4.justica_em_numeros_2016.enums.TipoRemessaEnum;

/**
 * Classe responsável por acessar dados da entidade Remessa
 * 
 * @author ivan.franca@trt6.jus.br
 */
public class RemessaDao extends DataJudBaseDao<Remessa> {

	/**
	 * Recupera a Remessa a partir da data de corte e do tipo da remessa.
	 * 
	 * @param dataCorte   data de corte da Remessa
	 * @param tipoRemessa tipo da Remessa
	 * @param fetchProcessoEnvio indica se deve ser realizado um fetch com processosEnvio
	 * @param fetchLote indica se deve ser realizado um fetch com lotes
	 * 
	 * @return uma instância de Remessa
	 */
	public Remessa getRemessa(LocalDate dataCorte, TipoRemessaEnum tipoRemessa, boolean fetchProcessoEnvio, boolean fetchLote) {
		StringBuilder hql = new StringBuilder();
		hql.append("select rm from Remessa rm ");
		if (fetchProcessoEnvio) {
			hql.append(" LEFT JOIN FETCH rm.processosEnvio pe ");
		}
		if (fetchLote) {
			hql.append(" LEFT JOIN FETCH rm.lotes lt ");
		}
		hql.append(" where rm.dataCorte = :dataCorte ");
		hql.append(" and rm.tipoRemessa = :tipoRemessa ");

		TypedQuery<Remessa> query = JPAUtil.getEntityManager().createQuery(hql.toString(), Remessa.class);
		query.setParameter("dataCorte", dataCorte);
		query.setParameter("tipoRemessa", tipoRemessa);

		return getSingleResultOrNull(query);
	}

}
