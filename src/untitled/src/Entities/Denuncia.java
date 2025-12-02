package Entities;

public class Denuncia {
    public int id;
    public String descricao;
    public int usuarioId;
    public int statusId;
    public int postoId;
    public int categoriaId;
    public int evidenciaId;

    public Denuncia(){};

    public Denuncia(String descricao, Usuario usuario, Status status, Posto posto, Categoria categoria, Evidencia evidencia) {
        this.descricao = descricao;
        this.usuarioId = usuario.id;
        this.statusId = status.id;
        this.postoId = posto.id;
        this.categoriaId = categoria.id;
        this.evidenciaId = evidencia.id;
    }
}
