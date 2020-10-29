package br.jus.trt4.justica_em_numeros_2016.dao;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.hibernate.Session;

import br.jus.trt4.justica_em_numeros_2016.entidades.BaseEntidade;

/**
 * Classe básica para acesso a dados
 * 
 * @author ivan.franca@trt6.jus.br
 */
public class DataJudBaseDao<T extends BaseEntidade> {

	protected Class<T> classe;

	@SuppressWarnings("unchecked")
	public DataJudBaseDao() {
		Type genericSuperclass = this.getClass().getGenericSuperclass();

		if (ParameterizedType.class.isInstance(genericSuperclass)) {
			ParameterizedType parameterizedType = ParameterizedType.class.cast(genericSuperclass);
			this.classe = (Class<T>) parameterizedType.getActualTypeArguments()[0];
		}
	}

	/**
	 * Método para gravar a entidade no banco de dados, através de uma inserção ou alteração.
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

	public void clear() {
		JPAUtil.getEntityManager().clear();
	}

	public void flush() {
		JPAUtil.getEntityManager().flush();
		;
	}

	/**
	 * Método para localizar uma única instância de um entidade, de acordo com sua chave primária.
	 * 
	 * @param idEntidade A chave principal (primary-key) da entidade.
	 * @return Uma instância da entidade.
	 */
	public T buscar(Serializable idEntidade) {
		return JPAUtil.getEntityManager().find(this.classe, idEntidade);
	}

	/**
	 * Alternativa para "javax.persistence.Query.getSingleResult()".
	 * 
	 * Método não lançará a exceção "NoResultException" (caso nenhum registro seja encontrado).
	 * 
	 * @param query A consulta que será executada.
	 * @return O único registro retornado na consulta, ou 'null' se não houver registro retornado.
	 */
	@SuppressWarnings("unchecked")
	public <E> E getSingleResultOrNull(Query query) {
		try {
			return (E) query.getSingleResult();
		} catch (NoResultException noResultException) {
			return null;
		}
	}
}
