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

    public boolean equals(BaseEmAnaliseEnum outro) {
        return (this.id.equals(outro.getId()));
    }

    public boolean isBasePJe() {
        return (this.equals(BaseEmAnaliseEnum.PJE));
    }

    public boolean isBaseLegado() {
        return (this.equals(BaseEmAnaliseEnum.LEGADO));
    }

}