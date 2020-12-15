package br.jus.trt4.justica_em_numeros_2016.util;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import br.jus.trt4.justica_em_numeros_2016.auxiliar.Auxiliar;
import br.jus.trt4.justica_em_numeros_2016.enums.Parametro;
import br.jus.trt4.justica_em_numeros_2016.enums.TipoRemessaEnum;

/**
 * 
 * @author ivan.franca@trt6.jus.br
 */
public class DataJudUtil {
	
	private static final Pattern P_CARGA_PROCESSO = Pattern.compile("^PROCESSO (\\d{7}\\-\\d{2}\\.\\d{4}\\.\\d\\.\\d{2}\\.\\d{4})$");
	private static final Pattern P_MES_ANO_CORTE = Pattern.compile("^(\\d+)-(\\d+)$");
	
	public static final String TIPO_CARGA = Auxiliar.getParametroConfiguracao(Parametro.tipo_carga_xml, true);
	public static final String MES_ANO_CORTE = Auxiliar.getParametroConfiguracao(Parametro.mes_ano_corte, true);
	
	private static Matcher getMatcherMesAno() {
		Matcher matcher = DataJudUtil.P_MES_ANO_CORTE.matcher(MES_ANO_CORTE);
		if (!matcher.find()) {
			throw new RuntimeException(
					"Parâmetro 'mes_ano_corte' não especifica corretamente o ano e o mês que precisam ser baixados! Verifique o arquivo 'config.properties'");
		}
		return matcher;
	}
	
	public static String getNumeroProcessoCargaProcesso(String tipoCarga) {
		Matcher m = DataJudUtil.P_CARGA_PROCESSO.matcher(tipoCarga);
		if (!m.find()) {
			throw new RuntimeException("Parâmetro 'tipo_carga_xml' não especifica corretamente o processo que precisa ser baixado! Verifique o arquivo 'config.properties'");
		}
		return m.group(1);
	}

	public static int getAnoCorte() {
		Matcher matcher = DataJudUtil.getMatcherMesAno();
		return Integer.parseInt(matcher.group(1));
	}

	public static int getMesCorte() {
		Matcher matcher = DataJudUtil.getMatcherMesAno();
		return Integer.parseInt(matcher.group(2));
	}
	
	public static String getDataPeriodoDeCorte(boolean returnarDataInicio) {
		String data = null;
		int anoCorte = DataJudUtil.getAnoCorte();
		int mesCorte = DataJudUtil.getMesCorte();
		if (returnarDataInicio) {
			data = anoCorte + "-" + mesCorte + "-1 00:00:00.000";
		} else {
			int maiorDiaNoMes = new GregorianCalendar(anoCorte, (mesCorte - 1), 1)
					.getActualMaximum(Calendar.DAY_OF_MONTH);
			data = anoCorte + "-" + mesCorte + "-" + maiorDiaNoMes + " 23:59:59.999";
		}
		return data;
	}
	
	public static LocalDate getDataCorte() {
		LocalDate dataCorte = LocalDate.now().withMonth(DataJudUtil.getMesCorte()).withYear(DataJudUtil.getAnoCorte());
		return dataCorte.withDayOfMonth(dataCorte.lengthOfMonth());
	}
	
	public static TipoRemessaEnum getTipoRemessa() {
		return TipoRemessaEnum.criarApartirDoLabel(TIPO_CARGA);
	}

}
