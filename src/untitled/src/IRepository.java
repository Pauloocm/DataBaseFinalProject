import Entities.*;
import Entities.View.Resumo;

import java.util.List;

public interface IRepository {

    void CreateDatabase() throws Exception;

    int GetLastId(String tableName) throws Exception;
    void CreateResumo()  throws Exception;
    void SetupTrigger() throws Exception;
    List<Resumo> GetResumo() throws Exception;

    // --- Endereco ---
    boolean AddEndereco(Endereco endereco) throws Exception;
    Endereco GetEnderecoById(int id) throws Exception;
    boolean UpdateEndereco(Endereco endereco) throws Exception;
    boolean DeleteEndereco(int id) throws Exception;

    // --- Usuario ---
    boolean AddUsuario(Usuario usuario) throws Exception;
    Usuario GetUsuarioById(int id) throws Exception;
    boolean UpdateUsuario(Usuario usuario) throws Exception;
    boolean DeleteUsuario(int id) throws Exception;

    // --- Categoria ---
    // Mantive AddCategory conforme seu código original, mas o ideal seria padronizar para AddCategoria
    boolean AddCategory(Categoria category) throws Exception;
    Categoria GetCategoriaById(int id) throws Exception;
    boolean UpdateCategoria(Categoria categoria) throws Exception;
    boolean DeleteCategoria(int id) throws Exception;

    // --- Bandeira ---
    boolean AddBandeira(Bandeira bandeira) throws Exception;
    Bandeira GetBandeiraById(int id) throws Exception;
    boolean UpdateBandeira(Bandeira bandeira) throws Exception;
    boolean DeleteBandeira(int id) throws Exception;

    // --- Status ---
    boolean AddStatus(Status status) throws Exception;
    Status GetStatusById(int id) throws Exception;
    boolean UpdateStatus(Status status) throws Exception;
    boolean DeleteStatus(int id) throws Exception;

    // --- Posto ---
    boolean AddPosto(Posto posto) throws Exception;
    Posto GetPostoById(int id) throws Exception;
    boolean UpdatePosto(Posto posto) throws Exception;
    boolean DeletePosto(int id) throws Exception;

    // --- Evidencia ---
    boolean AddEvidencia(Evidencia evidencia) throws Exception;
    Evidencia GetEvidenciaById(int id) throws Exception;
    boolean UpdateEvidencia(Evidencia evidencia) throws Exception;
    boolean DeleteEvidencia(int id) throws Exception;

    // --- Denuncia ---
    boolean AddDenuncia(Denuncia denuncia) throws Exception;
    Denuncia GetDenunciaById(int id) throws Exception;
    boolean UpdateDenuncia(Denuncia denuncia) throws Exception;
    boolean DeleteDenuncia(int id) throws Exception;
}