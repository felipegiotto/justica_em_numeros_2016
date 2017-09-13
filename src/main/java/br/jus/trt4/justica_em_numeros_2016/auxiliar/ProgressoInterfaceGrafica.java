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

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

public class ProgressoInterfaceGrafica {

	private static boolean tentouMontarInterface = false;
	private static boolean fecharJanelaAutomaticamente = true;
	private static JFrame janelaPrincipal;
	private static JPanel panelPrincipal;
	private static JScrollPane contentpane;
	private static JTextArea warnings;
	//	private static JButton botaoAbortar;
	private static AtomicInteger qtdTarefas = new AtomicInteger(0);
	private JProgressBar progressBar;
	private JPanel panelProgress;
	private JLabel labelTitulo;

	public ProgressoInterfaceGrafica(String titulo) {

		if (!tentouMontarInterface) {
			tentouMontarInterface = true;

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
				warnings = new JTextArea("(...)");
				warnings.setEditable(false);
				panelBarrasWarnings.add(warnings);

				// Painel inferior, com botão de abortar e checkbox de fechamento automático
				JPanel panelInferior = new JPanel(new GridLayout(1, 2));
				JCheckBox checkFecharAutomaticamente = new JCheckBox("Fechar janela ao terminar");
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
				System.out.println("Interface gráfica não está disponível. Utilizando somente texto.");
			}
		}

		qtdTarefas.incrementAndGet();

		if (janelaPrincipal != null) {

			// Cria um novo painel com barra de progresso
			panelProgress = new JPanel();
			panelProgress.setLayout(new GridLayout(1, 2));
			labelTitulo = new JLabel(titulo);
			panelProgress.add(labelTitulo);
			progressBar = new JProgressBar(0, 100);
			progressBar.setStringPainted(true);
			panelProgress.add(progressBar);
			panelPrincipal.add(panelProgress);
			panelPrincipal.setPreferredSize(new Dimension(0, 50));
			contentpane.validate();
		}
	}

	public void close() {

		// Tarefa fechada aparece em cinza
		if (labelTitulo != null) {
			labelTitulo.setForeground(Color.gray);
		}

		qtdTarefas.decrementAndGet();

		if (fecharJanelaAutomaticamente) {
			finalizarInterfaceGraficaSeTerminou();

		} else if (qtdTarefas.get() == 0) {
			//botaoAbortar.setEnabled(false);
			JOptionPane.showMessageDialog(null, "Operação finalizada! Veja arquivo de log para mais detalhes.");
		}
	}

	private void finalizarInterfaceGraficaSeTerminou() {
		if (qtdTarefas.get() == 0) {
			if (janelaPrincipal != null) {
				janelaPrincipal.dispose();
			}
		}
	}

	public void setProgress(int progress) {
		if (progressBar != null) {
			progressBar.setValue(progress);
		}
	}

	public synchronized void incrementProgress() {
		if (progressBar != null) {
			progressBar.setValue(progressBar.getValue() + 1);
		}
	}

	public void setMax(int max) {
		if (progressBar != null) {
			progressBar.setMaximum(max);
		}
	}

	public static void setWarnings(String texto) {
		if (warnings != null) {
			warnings.setText(texto);
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
