package br.jus.trt4.justica_em_numeros_2016.tasks;

import br.jus.trt4.justica_em_numeros_2016.dao.JPAUtil;

/**
 * Classe abstrata utilizada no controle de transação das operações.
 * 
 * @author ivan.franca@trt6.jus.br
 */
public abstract class OperacaoAbstract {
	//TODO: testar com uma carga completa e realizar commits parciais
	public void executarOperacao() {
		try {
			JPAUtil.iniciarTransacao();

			this.definirOperacao();

			JPAUtil.commit();
		} catch (Exception e) {
			JPAUtil.rollback();
		} finally {
			//JPAUtil.printEstatisticas();
			JPAUtil.close();
		}
	}

	public abstract void definirOperacao() throws Exception;

}
