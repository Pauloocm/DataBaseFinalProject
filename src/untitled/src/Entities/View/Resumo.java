package Entities.View;

public class Resumo {
    public String item;
    public Long quantidade;
    public String categoria;

    public Resumo(String item, Long quantidade, String categoria) {
        this.item = item;
        this.quantidade = quantidade;
        this.categoria = categoria;
    }

    public Resumo() {}
}
