package br.jus.trt4.justica_em_numeros_2016.dao;

import java.time.LocalDate;

import javax.persistence.TypedQuery;

import br.jus.trt4.justica_em_numeros_2016.entidades.Lote;
import br.jus.trt4.justica_em_numeros_2016.enums.TipoRemessaEnum;

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
     * 
     * @return uma instância do Lote
     */
	public Lote getUltimoLoteDeUmaRemessa(LocalDate dataCorte, TipoRemessaEnum tipoRemessa) {
		StringBuilder hql = new StringBuilder();
		hql.append("select lot from Lote lot ");
		hql.append(" where lot.remessa.dataCorte = :dataCorte ");
		hql.append(" and lot.remessa.tipoRemessa = :tipoRemessa ");
		hql.append(" and lot.numero = ( select MAX(lotm.numero) from Lote lotm ");
		hql.append(" where lotm.remessa.dataCorte = :dataCorte ");
		hql.append(" and lotm.remessa.tipoRemessa = :tipoRemessa)");

		TypedQuery<Lote> query = JPAUtil.getEntityManager().createQuery(hql.toString(), Lote.class);
		query.setParameter("dataCorte", dataCorte);
		query.setParameter("tipoRemessa", tipoRemessa);

		return getSingleResultOrNull(query);
	}


}
