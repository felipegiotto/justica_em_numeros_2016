package br.jus.trt4.justica_em_numeros_2016.enums;

import br.jus.trt4.justica_em_numeros_2016.auxiliar.DataJudException;

public enum TipoTipoComplementoCNJ {
	
    LIVRE,
    IDENTIFICADOR,
    TABELADO;
    
    public static TipoTipoComplementoCNJ getTipoTipoComplemento(String strTipoTipoComplemento) throws DataJudException{
        if("L".equals(strTipoTipoComplemento)) {
            return LIVRE;
        } else if ("I".equals(strTipoTipoComplemento)) {
            return IDENTIFICADOR;
        } else if ("T".equals(strTipoTipoComplemento)) {
            return TABELADO;
        } else {
            throw new DataJudException("Tipo de complemento CNJ com tipo desconhecido.");
        }
    }
    
    public boolean isComplementoTabelado() {
        return TipoTipoComplementoCNJ.TABELADO.equals(this);
    }

}