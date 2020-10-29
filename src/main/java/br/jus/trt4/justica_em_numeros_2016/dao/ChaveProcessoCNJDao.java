package br.jus.trt4.justica_em_numeros_2016.dao;

import javax.persistence.TypedQuery;

import br.jus.trt4.justica_em_numeros_2016.entidades.ChaveProcessoCNJ;

/**
 * Classe responsável por acessar dados da entidade ChaveProcessoCNJ
 * 
 * @author ivan.franca@trt6.jus.br
 * 
 */
public class ChaveProcessoCNJDao extends DataJudBaseDao<ChaveProcessoCNJ> {
	
	/**
     * Recupera uma ChaveProcessoCNJ a partir das seguintes informações: número do processo, código da classe judicial, 
     * código do órgão julgador e grau.
     * 
     * @param numeroProcesso 
     * @param codigoClasseJudicial 
     * @param codigoOrgaoJulgador 
     * @param grau 
     * 
     * @return uma instância da ChaveProcessoCNJ
     */
	public ChaveProcessoCNJ getChaveProcessoCNJ(String numeroProcesso, String codigoClasseJudicial, Long codigoOrgaoJulgador, String grau) {
		StringBuilder hql = new StringBuilder();
		hql.append("select cp from ChaveProcessoCNJ cp ");
		hql.append(" where cp.numeroProcesso = :numeroProcesso");
		hql.append(" and cp.codigoClasseJudicial = :codigoClasseJudicial");
		hql.append(" and cp.codigoOrgaoJulgador = :codigoOrgaoJulgador");
		hql.append(" and cp.grau = :grau");
		TypedQuery<ChaveProcessoCNJ> query = JPAUtil.getEntityManager().createQuery(hql.toString(), ChaveProcessoCNJ.class);
		query.setParameter("numeroProcesso", numeroProcesso);
		query.setParameter("codigoClasseJudicial", codigoClasseJudicial);
		query.setParameter("codigoOrgaoJulgador", codigoOrgaoJulgador);
		query.setParameter("grau", grau);
		return getSingleResultOrNull(query);
	}

}
