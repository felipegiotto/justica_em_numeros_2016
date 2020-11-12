package br.jus.trt4.justica_em_numeros_2016.dao;

import javax.persistence.TypedQuery;

import br.jus.trt4.justica_em_numeros_2016.entidades.Lote;
import br.jus.trt4.justica_em_numeros_2016.entidades.Remessa;

/**
 * Classe responsável por acessar dados da entidade Lote
 * 
 * @author ivan.franca@trt6.jus.br
 */
public class LoteDao extends DataJudBaseDao<Lote> {
	
	/**
     * Recupera o último Lote de uma Remessa a partir da data de corte e do tipo da remessa.
     * 
     * @param dataCorte data de corte da Remessa
     * @param tipoRemessa tipo da Remessa
     * @param fetchLoteProcesso indica se deve ser realizado um fetch com lotesProcessos e chaveProcessoCNJ
     * 
     * @return uma instância do Lote
     */
	public Lote getUltimoLoteDeUmaRemessa(Remessa remessa, boolean fetchLoteProcesso) {
		StringBuilder hql = new StringBuilder();
		hql.append("select lot from Lote lot ");
		if (fetchLoteProcesso) {
			hql.append(" LEFT JOIN FETCH lot.lotesProcessos lp ");	
			hql.append(" LEFT JOIN FETCH lp.chaveProcessoCNJ ch ");	
		}
		hql.append(" where lot.remessa.id = :idRemessa ");
		hql.append(" and lot.numero = ( select MAX(lotm.numero) from Lote lotm ");
		hql.append(" where lotm.remessa.id = :idRemessa )");

		TypedQuery<Lote> query = JPAUtil.getEntityManager().createQuery(hql.toString(), Lote.class);
		query.setParameter("idRemessa", remessa.getId());

		return getSingleResultOrNull(query);
	}

}
