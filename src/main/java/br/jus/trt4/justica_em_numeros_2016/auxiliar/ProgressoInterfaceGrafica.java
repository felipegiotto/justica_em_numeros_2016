package br.jus.trt4.justica_em_numeros_2016.auxiliar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import javax.swing.border.MatteBorder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Classe responsável por exibir uma interface gráfica que lista o progresso de cada operação sendo executada.
 * 
 * Cada operação deve instanciar um objeto desta classe, informar o progresso da operação e, ao final, chamar o método "close".
 * 
 * @author felipe.giotto@trt4.jus.br
 */
public class ProgressoInterfaceGrafica {

	private static final Logger LOGGER = LogManager.getLogger(ProgressoInterfaceGrafica.class);
	private static boolean tentarMontarInterfaceAWT = true;
	private static boolean fecharJanelaAutomaticamente;
	private static JFrame janelaPrincipal;
	private static JPanel panelPrincipal;
	private static JScrollPane contentpane;
	private static JTextArea warnings;
	//	private static JButton botaoAbortar;
	private static AtomicInteger qtdTarefas = new AtomicInteger(0);
	private JProgressBar progressBar;
	private JPanel panelProgress;
	private JLabel labelTitulo;
	private JLabel labelInformacoes;

	public ProgressoInterfaceGrafica(String titulo) {

		// Tenta montar a interface gráfica somente uma vez. Se o usuário estiver rodando em um ambiente
		// sem interface gráfica, não previsa ficar tentando montar a cada nova operação.
		if (tentarMontarInterfaceAWT) {
			tentarMontarInterfaceAWT = false;

			try {
				// Janela principal
				janelaPrincipal = new JFrame("Justiça em Números");
				janelaPrincipal.setPreferredSize(new Dimension(700, 600));
				janelaPrincipal.setSize(700, 600);
				janelaPrincipal.setLayout(new BorderLayout());
				janelaPrincipal.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
				janelaPrincipal.addWindowListener(new WindowAdapter() {
					public void windowClosing(java.awt.event.WindowEvent evt){
						finalizarInterfaceGraficaSeTerminou();
					}
				});

				// Painel que conterá, em cima, as barras de progresso, e abaixo, os warnings
				JPanel panelBarrasWarnings = new JPanel(new GridLayout(2, 1));

				// Painel onde serão inseridas as tarefas
				panelPrincipal = new JPanel(new GridLayout(0, 1));
				contentpane = new JScrollPane(panelPrincipal);
				panelBarrasWarnings.add(contentpane);
				janelaPrincipal.add(panelBarrasWarnings);

				// TextArea onde serão exibidos os warnings encontrados.
				warnings = new JTextArea("");
				warnings.setEditable(false);
				panelBarrasWarnings.add(warnings);

				// Painel inferior, com botão de abortar e checkbox de fechamento automático
				JPanel panelInferior = new JPanel(new GridLayout(1, 2));
				
				// Checkbox de fechamento automático
				JCheckBox checkFecharAutomaticamente = new JCheckBox("Fechar janela ao terminar");
				try {
					fecharJanelaAutomaticamente = Auxiliar.getParametroBooleanConfiguracao(Parametro.interface_grafica_fechar_automaticamente, false);
				} catch (Exception ex) { }
				checkFecharAutomaticamente.setSelected(fecharJanelaAutomaticamente);
				checkFecharAutomaticamente.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						fecharJanelaAutomaticamente = ((JCheckBox) (e.getSource())).isSelected();
					}
				});
				panelInferior.add(checkFecharAutomaticamente);
				//		        botaoAbortar = new JButton("Abortar");
				//		        panelInferior.add(botaoAbortar);


				janelaPrincipal.add(panelInferior, BorderLayout.SOUTH);

				// Exibe a janela principal
				janelaPrincipal.setVisible(true);

			} catch (HeadlessException ex) {
				
				// Se o Swing não estiver disponível
				LOGGER.info("Interface gráfica não está disponível. Utilizando somente texto.");
			}
		}

		qtdTarefas.incrementAndGet();

		// Só cria os demais componentes se conseguiu criar a interface principal
		if (janelaPrincipal != null) {

			// Cria um novo painel com título da tarefa e barra de progresso
			panelProgress = new JPanel();
			panelProgress.setBorder(new MatteBorder(1, 0, 0, 0, Color.gray));
			panelProgress.setLayout(new GridLayout(1, 2));
			
			// Painel que conterá o título da tarefa e uma linha de informações adicional
			JPanel panelEsquerdo = new JPanel();
			panelEsquerdo.setLayout(new GridLayout(2, 1));
			panelProgress.add(panelEsquerdo);
			
			// Label com o título da tarefa
			labelTitulo = new JLabel(titulo);
			panelEsquerdo.add(labelTitulo);
			
			// Label com informações extras sobre a tarefa
			labelInformacoes = new JLabel("");
			panelEsquerdo.add(labelInformacoes);
			
			// Barra de progresso
			progressBar = new JProgressBar(0, 100);
			progressBar.setStringPainted(true);
			panelProgress.add(progressBar);
			
			// Adiciona tudo na janela principal
			panelPrincipal.add(panelProgress);
			panelPrincipal.setPreferredSize(new Dimension(0, 50));
			contentpane.validate();
		}
	}

	/**
	 * Define o valor atual da barra de progresso (valor entre "0" e o definido por "setMax()" - padrão 100)
	 */
	public void setProgress(int progress) {
		if (progressBar != null) {
			progressBar.setValue(progress);
		}
	}

	/**
	 * Incrementa o progresso atual em 1
	 */
	public synchronized void incrementProgress() {
		if (progressBar != null) {
			progressBar.setValue(progressBar.getValue() + 1);
		}
	}

	/**
	 * Define o valor máximo da barra de progresso
	 * 
	 * @param max
	 */
	public void setMax(int max) {
		if (progressBar != null) {
			progressBar.setMaximum(max);
		}
	}

	public static boolean isTentarMontarInterface() {
		return tentarMontarInterfaceAWT;
	}
	
	public static void setTentarMontarInterface(boolean tentarMontarInterface) {
		ProgressoInterfaceGrafica.tentarMontarInterfaceAWT = tentarMontarInterface;
	}
	
	/**
	 * Define um texto a ser exibido logo abaixo do nome da tarefa
	 * 
	 * Pode ser utilizado para mostrar o nome de subtarefas e/ou o tempo restante
	 * 
	 * @param texto
	 */
	public void setInformacoes(String texto) {
		if (labelInformacoes != null) {
			labelInformacoes.setText(texto);
		}
	}

	/**
	 * Indica que a tarefa não está mais sendo executada
	 */
	public void close() {

		// Tarefa fechada aparece em cinza
		if (labelTitulo != null) {
			labelTitulo.setForeground(Color.gray);
		}

		qtdTarefas.decrementAndGet();

		if (fecharJanelaAutomaticamente) {
			finalizarInterfaceGraficaSeTerminou();

		} else if (qtdTarefas.get() == 0 && janelaPrincipal != null) {
			
			//botaoAbortar.setEnabled(false);
			JOptionPane.showMessageDialog(null, "Operação finalizada! Veja arquivo de log para mais detalhes.");
		}
	}

	/**
	 * Define um texto a ser exibido no text area que aparece abaixo das barras de 
	 * progresso das operações individuais
	 * 
	 * @param texto
	 */
	public static void setWarnings(String texto) {
		if (warnings != null) {
			warnings.setText(texto);
		}
	}
	
	/**
	 * Se não houver mais nenhuma tarefa em execução, fecha a interface gráfica
	 */
	private void finalizarInterfaceGraficaSeTerminou() {
		if (qtdTarefas.get() == 0) {
			if (janelaPrincipal != null) {
				janelaPrincipal.dispose();
			}
		}
	}

	public static void main(String[] args) throws Exception {
		ProgressoInterfaceGrafica op1 = new ProgressoInterfaceGrafica("Tarefa 1");
		try {
			Thread.sleep(2000);

			ProgressoInterfaceGrafica op2 = new ProgressoInterfaceGrafica("Tarefa 2");
			try {
				op2.setMax(200);
				op2.setProgress(40);
				Thread.sleep(2000);
			} finally {
				op2.close();
			}

			ProgressoInterfaceGrafica op3 = new ProgressoInterfaceGrafica("Tarefa 3");
			try {
				op3.setProgress(100);
				Thread.sleep(2000);
			} finally {
				op3.close();
			}

		} finally {
			op1.close();
		}
	}
}
