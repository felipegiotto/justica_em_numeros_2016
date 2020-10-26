package br.jus.trt4.justica_em_numeros_2016.dao;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

public class JPAUtil {
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

}
