package Entities;

public class Endereco {
    public int id;
    public String uf;
    public String cidade;
    public String bairro;
    public String rua;

    public Endereco(){};

    public Endereco(String uf, String cidade, String bairro, String rua) {
        this.uf = uf;
        this.cidade = cidade;
        this.bairro = bairro;
        this.rua = rua;
    }
}
