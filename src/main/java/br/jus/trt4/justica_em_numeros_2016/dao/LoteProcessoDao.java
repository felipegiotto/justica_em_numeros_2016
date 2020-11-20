package br.jus.trt4.justica_em_numeros_2016.dao;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.TypedQuery;

import br.jus.trt4.justica_em_numeros_2016.entidades.Lote;
import br.jus.trt4.justica_em_numeros_2016.entidades.LoteProcesso;
import br.jus.trt4.justica_em_numeros_2016.entidades.Remessa;
import br.jus.trt4.justica_em_numeros_2016.enums.SituacaoLoteProcessoEnum;

/**
 * Classe responsável por acessar dados da entidade LoteProcesso
 * 
 * @author ivan.franca@trt6.jus.br
 */
public class LoteProcessoDao extends DataJudBaseDao<LoteProcesso> {

	public List<LoteProcesso> getLoteProcesso(Lote lote) {
		List<LoteProcesso> retorno = new ArrayList<LoteProcesso>();
		if (lote.getId() != null) {
			StringBuilder hql = new StringBuilder();
			hql.append("select lp from LoteProcesso lp ");
			hql.append(" LEFT JOIN FETCH lp.chaveProcessoCNJ ch ");	
			hql.append(" where lp.lote.id = :idLote ");

			TypedQuery<LoteProcesso> query = JPAUtil.getEntityManager().createQuery(hql.toString(), LoteProcesso.class);
			query.setParameter("idLote", lote.getId());
			retorno = query.getResultList();
		}

		return retorno;
	}
	
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
		hql.append(" and lp.situacao IN ( :situacoes ) ");
		
		TypedQuery<LoteProcesso> query = JPAUtil.getEntityManager().createQuery(hql.toString(), LoteProcesso.class);
		query.setParameter("idLote", lote.getId());
		query.setParameter("situacoes", situacoes);

		return query.getResultList();
	}
	
	public Long getQuantidadeProcessosPorLoteESituacao(Lote lote, List<SituacaoLoteProcessoEnum> situacoes) {
		StringBuilder hql = new StringBuilder();
		hql.append("select count(lp) from LoteProcesso lp ");
		hql.append(" where lp.lote.id = :idLote ");
		hql.append(" and lp.situacao IN ( :situacoes ) ");
		
		TypedQuery<Long> query = JPAUtil.getEntityManager().createQuery(hql.toString(), Long.class);
		query.setParameter("idLote", lote.getId());
		query.setParameter("situacoes", situacoes);
		
		return getSingleResultOrNull(query);
	}
	
	public List<LoteProcesso> getProcessosPorRemessaESituacao(Remessa remessa, List<SituacaoLoteProcessoEnum> situacoes) {
		StringBuilder hql = new StringBuilder();
		hql.append("select lp from LoteProcesso lp ");
		hql.append(" LEFT JOIN FETCH lp.chaveProcessoCNJ ch ");	
		hql.append(" where lp.lote.remessa.id = :idRemessa ");
		hql.append(" and lp.situacao IN ( :situacoes ) ");
		
		TypedQuery<LoteProcesso> query = JPAUtil.getEntityManager().createQuery(hql.toString(), LoteProcesso.class);
		query.setParameter("idRemessa", remessa.getId());
		query.setParameter("situacoes", situacoes);

		return query.getResultList();
	}
	
	public Long getQuantidadeProcessosPorRemessaESituacao(Remessa remessa, List<SituacaoLoteProcessoEnum> situacoes) {
		StringBuilder hql = new StringBuilder();
		hql.append("select count(lp) from LoteProcesso lp ");
		hql.append(" where lp.lote.remessa.id = :idRemessa ");
		hql.append(" and lp.situacao IN ( :situacoes ) ");
		
		TypedQuery<Long> query = JPAUtil.getEntityManager().createQuery(hql.toString(), Long.class);
		query.setParameter("idRemessa", remessa.getId());
		query.setParameter("situacoes", situacoes);
		
		return getSingleResultOrNull(query);
	}
	
	public List<LoteProcesso> getProcessosComXMLPorIDs(List<Long> ids) {
		StringBuilder hql = new StringBuilder();
		hql.append("select lp from LoteProcesso lp ");
		hql.append(" LEFT JOIN FETCH lp.chaveProcessoCNJ ch ");	
		hql.append(" LEFT JOIN FETCH lp.xmlProcesso xm ");	
		hql.append(" where lp.id IN ( :ids ) ");
		
		TypedQuery<LoteProcesso> query = JPAUtil.getEntityManager().createQuery(hql.toString(), LoteProcesso.class);
		query.setParameter("ids", ids);

		return query.getResultList();
	}
	
	
	
}
