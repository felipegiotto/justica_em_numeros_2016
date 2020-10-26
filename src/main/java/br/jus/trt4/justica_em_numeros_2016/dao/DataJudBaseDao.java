package br.jus.trt4.justica_em_numeros_2016.dao;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import javax.persistence.TypedQuery;

import org.hibernate.Session;

import br.jus.trt4.justica_em_numeros_2016.entidades.BaseEntidade;
import br.jus.trt4.justica_em_numeros_2016.util.StringUtil;

/**
 * Classe básica para acesso a dados
 * 
 */
public class DataJudBaseDao<T extends BaseEntidade> {

	protected Class<T> classeEntidade;

	/**
	 * Construtor padrão.
	 */
	@SuppressWarnings("unchecked")
	public DataJudBaseDao() {
		Type genericSuperclass = this.getClass().getGenericSuperclass();

		if (ParameterizedType.class.isInstance(genericSuperclass)) {
			ParameterizedType parameterizedType = ParameterizedType.class.cast(genericSuperclass);
			this.classeEntidade = (Class<T>) parameterizedType.getActualTypeArguments()[0];
		}
	}

	/**
	 * Método para gravar a entidade no banco de dados, através de uma inserção ou
	 * alteração.
	 * 
	 * @param entidade que se deseja persistir.
	 */
	public void incluirOuAlterar(T entidade) {
		JPAUtil.getEntityManager().unwrap(Session.class).saveOrUpdate(entidade);
	}

	/**
	 * Método para persistir uma instância da entidade no banco de dados.
	 * 
	 * @param entidade A instância da entidade que deseja persistir.
	 */
	public void incluir(T entidade) {
		JPAUtil.getEntityManager().persist(entidade);
	}

	/**
	 * Método para excluir uma instãncia da entidade no banco de dados .
	 * 
	 * @param entidade A instância da entidade a ser excluída.
	 */
	public void excluir(T entidade) {
		JPAUtil.getEntityManager().remove(entidade);
	}

	/**
	 * Método para alterar uma instância da entidade.
	 * 
	 * @param entidade A instância da entidade a ser alterada.
	 * @return A instancia da entidade anexada ao gerenciador de persistência.
	 */
	public T alterar(T entidade) {
		return JPAUtil.getEntityManager().merge(entidade);
	}

	/**
	 * Método para obter a lista padrão de registros de uma entidade.
	 *
	 * @return Lista de instâncias da entidade.
	 */
	public List<T> listar() {
		TypedQuery<T> query = JPAUtil.getEntityManager()
				.createQuery(this.criarComandoPesquisaEntidade(this.classeEntidade, "entidade"), this.classeEntidade);
		return query.getResultList();
	}

	/**
	 * Cria o comando de pesquisa a ser usado para pesquisa de um entidade.
	 *
	 * @param classe
	 * @param aliasEntidade
	 * @return
	 */
	protected String criarComandoPesquisaEntidade(Class classe, String aliasEntidade) {
		StringBuilder comando = new StringBuilder();
		comando.append("FROM ").append(classe.getCanonicalName()).append(StringUtil.CARACTER_ESPACO)
				.append(aliasEntidade).append(StringUtil.CARACTER_ESPACO);
		return comando.toString();
	}

	/**
	 * 
	 * @param aliasEntidade
	 * @return
	 */
	protected String criarComandoPesquisaEntidade(String aliasEntidade) {
		return this.criarComandoPesquisaEntidade(this.classeEntidade, aliasEntidade);
	}

	/**
	 * Método para localizar uma única instância de um entidade, de acordo com sua
	 * chave primária.
	 * 
	 * @param idEntidade A chave principal (primary-key) da entidade.
	 * @return Uma instância da entidade.
	 */
	public T buscar(Serializable idEntidade) {
		return JPAUtil.getEntityManager().find(this.classeEntidade, idEntidade);
	}

}
