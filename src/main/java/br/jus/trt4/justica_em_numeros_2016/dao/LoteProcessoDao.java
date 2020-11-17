package br.jus.trt4.justica_em_numeros_2016.dao;

import java.util.List;

import javax.persistence.TypedQuery;

import br.jus.trt4.justica_em_numeros_2016.entidades.Lote;
import br.jus.trt4.justica_em_numeros_2016.entidades.LoteProcesso;
import br.jus.trt4.justica_em_numeros_2016.enums.SituacaoLoteProcessoEnum;

/**
 * Classe responsável por acessar dados da entidade LoteProcesso
 * 
 * @author ivan.franca@trt6.jus.br
 */
public class LoteProcessoDao extends DataJudBaseDao<LoteProcesso> {

	
	public Long getQuantidadeProcessosPorLote(Lote lote) {
		StringBuilder hql = new StringBuilder();
		hql.append("select count(lp) from LoteProcesso lp ");
		hql.append(" where lp.lote.id = :idLote ");

		TypedQuery<Long> query = JPAUtil.getEntityManager().createQuery(hql.toString(), Long.class);
		query.setParameter("idLote", lote.getId());

		return getSingleResultOrNull(query);
	}
	
	public List<LoteProcesso> getProcessosPorLoteESituacao(Lote lote, List<SituacaoLoteProcessoEnum> situacoes) {
		StringBuilder hql = new StringBuilder();
		hql.append("select lp from LoteProcesso lp ");
		hql.append(" where lp.lote.id = :idLote ");
		hql.append(" and lp.situacao IN ( :situacoes )  ");
		
		TypedQuery<LoteProcesso> query = JPAUtil.getEntityManager().createQuery(hql.toString(), LoteProcesso.class);
		query.setParameter("idLote", lote.getId());
		query.setParameter("situacoes", situacoes);

		return query.getResultList();
	}
	
	public Long getQuantidadeProcessosPorLoteESituacao(Lote lote, List<SituacaoLoteProcessoEnum> situacoes) {
		StringBuilder hql = new StringBuilder();
		hql.append("select count(lp) from LoteProcesso lp ");
		hql.append(" where lp.lote.id = :idLote ");
		hql.append(" and lp.situacao IN ( :situacoes )  ");
		
		TypedQuery<Long> query = JPAUtil.getEntityManager().createQuery(hql.toString(), Long.class);
		query.setParameter("idLote", lote.getId());
		query.setParameter("situacoes", situacoes);
		
		return getSingleResultOrNull(query);
	}
	
}
