package br.jus.trt4.justica_em_numeros_2016.dao;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

/**
 * Classe utilitária que será utilizada no controle de transações da JPA.
 * 
 * @author ivan.franca@trt6.jus.br
 *
 */
public class JPAUtil {
	private static final Logger LOGGER = LogManager.getLogger(JPAUtil.class);
	private static final EntityManagerFactory emf = Persistence.createEntityManagerFactory("emDatajud");
	private static EntityManager entityManager;

	public static EntityManager getEntityManager() {
		if (entityManager == null) {
			entityManager = emf.createEntityManager();
		}
		return entityManager;
	}

	/**
	 * Inicia um bloco transacional para operações na base de dados
	 */
	public static void iniciarTransacao() {
		JPAUtil.getEntityManager().getTransaction().begin();
	}

	/**
	 * Realiza commit e encerra a transação.
	 */
	public static void commit() {
		EntityTransaction transaction = JPAUtil.getEntityManager().getTransaction();
		if (transaction != null && transaction.isActive()) {
			transaction.commit();
		}
	}

	/**
	 * Realiza rollback e encerra a transação
	 */
	public static void rollback() {
		EntityTransaction transaction = JPAUtil.getEntityManager().getTransaction();
		if (transaction != null && transaction.isActive()) {
			transaction.rollback();
		}
	}

	public static void close() {
		if (JPAUtil.getEntityManager() != null) {
			JPAUtil.getEntityManager().close();
		}
	}
	
	public static void clear() {
		JPAUtil.getEntityManager().clear();
	}
	
	public static void flush() {
		JPAUtil.getEntityManager().flush();;
	}

	public static void destroy() {
		JPAUtil.emf.close();
	}
	
	public static void printEstatisticas() {
		Session session = (Session) JPAUtil.getEntityManager().getDelegate();
		SessionFactory sessionFactory = session.getSessionFactory();
		Statistics estatisticas = sessionFactory.getStatistics();

		LOGGER.info("Qtde. de entidades buscadas: " + estatisticas.getEntityFetchCount());
		LOGGER.info("Qtde. de entidades carregadas: " + estatisticas.getEntityLoadCount());
		LOGGER.info("Qtde. de listas buscadas: " + estatisticas.getCollectionFetchCount());
		LOGGER.info("Qtde. de listas carregadas: " + estatisticas.getCollectionLoadCount());
		double queryCacheHitCount = estatisticas.getQueryCacheHitCount();
		double queryCacheMissCount = estatisticas.getQueryCacheMissCount();
		double totalQueries = queryCacheHitCount + queryCacheMissCount;
		double queryCacheHitRatio = (totalQueries == 0) ? 0 : queryCacheHitCount / totalQueries;
		LOGGER.info("Qtde de consultas encontradas no cache: " + queryCacheHitCount);
		LOGGER.info("Qtde de consultas fora do cache: " + queryCacheMissCount);
		LOGGER.info("Proporção de acerto do cache: " + queryCacheHitRatio);
		LOGGER.info("Qtde de consultas executadas: " + estatisticas.getQueryExecutionCount());
		String[] queries = estatisticas.getQueries();
		for (int i = 0; i < queries.length; i++) {
			LOGGER.info("Consulta " + i + ": " + queries[i]);
		}
		LOGGER.info("Query mais lenta: " + estatisticas.getQueryExecutionMaxTimeQueryString());
		estatisticas.clear();
	}

}
