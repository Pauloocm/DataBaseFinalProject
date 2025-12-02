package Entities;

public class Posto {
    public int id;
    public String razaoSocial;
    public String cnpj;
    public Endereco endereco;
    public Bandeira bandeira;

    public Posto(String razaoSocial, String cnpj, Endereco endereco,  Bandeira bandeira) {
        this.razaoSocial = razaoSocial;
        this.cnpj = cnpj;
        this.endereco = endereco;
        this.bandeira = bandeira;
    }

    public Posto(){};
}
