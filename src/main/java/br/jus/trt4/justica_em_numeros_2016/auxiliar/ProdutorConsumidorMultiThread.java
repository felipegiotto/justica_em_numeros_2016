package br.jus.trt4.justica_em_numeros_2016.auxiliar;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Classe que implementa um sistema de Produtor->Consumidor, onde pode-se definir quantas threads 
 * rodarão o consumidor em paralelo.
 * 
 * A cada chamada ao método produzir(), a classe chamará o método consumir(), tudo isso em um ambiente
 * multi-thread controlado.
 * 
 * Enquanto a fila não estiver muito grande (conforme parâmetro tamanhoMaximoFila), o método produzir()
 * irá retornar imediatamente, permitindo que outros objetos sejam processados.
 * 
 * Se a fila ficar muito grande e os consumidores não conseguirem mais dar conta, o método produzir()
 * não irá retornar imediatamente, sendo que irá aguardar até que algum registro seja processado.
 * 
 * IMPORTANTE: depois de produzir todos os itens, chamar o método "aguardarTermino()", que garantirá
 * a execução até o final.
 * 
 * @param <T>: tipo de objeto que será produzido e consumido.
 * 
 * @author fgiotto
 */
public abstract class ProdutorConsumidorMultiThread<T> {

	private static final Logger LOGGER = LogManager.getLogger(ProdutorConsumidorMultiThread.class);
	private LinkedBlockingQueue<T> queue;
	
	// Indica se a instância ainda está ativa, ou seja, recebendo novos objetos com o método "produzir()".
	private boolean isRunning = true;
	private RuntimeException exception = null;
	private String nameForDebug = null;
	private Object MUTEX_THREADS_RUNNING = new Object();
	private int threadsRunning = 0;
	
	/**
	 * Método que deve ser sobrescrito ao instanciar essa classe. Esse método deverá consumir o objeto
	 * que foi chamado no método produzir().
	 * 
	 * @param objeto
	 */
	public abstract void consumir(T objeto);
	
	public ProdutorConsumidorMultiThread(int tamanhoMaximoFila, int qtdThreadsSimultaneas, int threadPriority, String name) {
		setNameForDebug(name);
		init(tamanhoMaximoFila, qtdThreadsSimultaneas, threadPriority);
	}
	
	public ProdutorConsumidorMultiThread(int tamanhoMaximoFila, int qtdThreadsSimultaneas, int threadPriority) {
		init(tamanhoMaximoFila, qtdThreadsSimultaneas, threadPriority);
	}
	
	private void init(int tamanhoMaximoFila, int qtdThreadsSimultaneas, int threadPriority) {
		debug("Iniciando ProdutorConsumidorMultiThread com tamanhoMaximoFila=" + tamanhoMaximoFila + ", qtdThreadsSimultaneas=" + qtdThreadsSimultaneas);
		queue = new LinkedBlockingQueue<T>(tamanhoMaximoFila);

		// Inicia a quantidade de threads desejada para processar a lista de itens
		threadsRunning = qtdThreadsSimultaneas;
		for (int i=0; i<qtdThreadsSimultaneas; i++) {
			new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						while (true) {

							boolean estavaRodandoAntesDoPoll = isRunning;
							
							// Carrega um item da fila, aguardando até um tempo limite, pois pode ser que
							// o processamento tenha terminado.
							T item = queue.poll(500, TimeUnit.MILLISECONDS);

							// Se encontrou um item na fila de processamento, consome.
							if (item != null) {
								try {
									consumir(item);
								} catch (RuntimeException e) {
									LOGGER.error((nameForDebug != null ? nameForDebug + ": " : "") + "Erro no consumidor do item " + item + ": " + e.getMessage(), e);
									exception = e;
								}
								
							} else {
								
								// Se nao encontrou um item na fila de processamento, verifica se é porque a operação terminou.
								// Se for, aborta a thread.
								if (!estavaRodandoAntesDoPoll) {
									return;
								}
							}
						}
					} catch (Throwable ex) {
						LOGGER.error(ex);
						
					} finally {
						synchronized (MUTEX_THREADS_RUNNING) {
							threadsRunning--;
						}
					}
				}
			}).start();
		}	
	}

	private void debug(String debugText) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug((nameForDebug != null ? nameForDebug + ": " : "") + debugText);
		}
	}
	
	/**
	 * Método que deve ser chamado, indicando que um novo objeto "T" deve ser consumido.
	 * 
	 * Ver descrição da classe a respeito do controle de concorrência e tamanho de fila.
	 *  
	 * @param objeto
	 * @throws InterruptedException
	 * @return true se o objeto foi inserido na fila OU false se houve alguma exception
	 * em alguma outra chamada e, por isso, o objeto não foi inserido na fila.
	 */
	public boolean produzir(T objeto) throws InterruptedException  {
		
		if (exception != null) {
			return false;
		}
		
		debug("Produzir ANTES: " + queue.size());
		queue.put(objeto);
		debug("Produzir DEPOIS: " + queue.size());
		return true;
	}
	
	/**
	 * Método que deve ser chamado, obrigatoriamente, quando nenhum elemento mais
	 * será entregue ao método produzir().
	 * 
	 * Isso fará com que o objeto aguarde o processamento de tudo que está atualmente na fila.
	 * 
	 * @throws RuntimeException: Se ocorrer algum erro durante o processamento de algum dos objetos.
	 * @throws InterruptedException: Se a thread atual, que está aguardando o término do processamento, for interrompida.
	 */
	public void aguardarTermino() throws RuntimeException {
		isRunning = false;
		while(threadsRunning > 0) {
			debug("Aguardando termino das threads");
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// Continuar esperando!
			}
		}
		
		if (exception != null) {
			throw exception;
		}
	}
	
	public void setNameForDebug(String name) {
		this.nameForDebug = name;
	}
}
