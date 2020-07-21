package br.jus.trt4.justica_em_numeros_2016.enums;

public enum BaseEmAnaliseEnum {

    PJE("PJe", 1),
    LEGADO("Sistema Judicial Legado", 2);

    private String descricao;
    private Integer id;


    private BaseEmAnaliseEnum(String descricao, Integer id) {
        this.descricao = descricao;
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean isBasePJe() {
        return BaseEmAnaliseEnum.PJE.equals(this);
    }

    public boolean isBaseLegado() {
        return BaseEmAnaliseEnum.LEGADO.equals(this);
    }

}