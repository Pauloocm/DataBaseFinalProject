package Entities;

public class Usuario {
    public int id;
    public String nome;
    public String email;
    public Endereco endereco;
    public int TotalDenuncias;

    public Usuario(){};

    public Usuario(String nome, String email, Endereco endereco) {
        this.nome = nome;
        this.email = email;
        this.endereco = endereco;
    }
}
