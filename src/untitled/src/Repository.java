import Entities.*;
import Entities.View.Resumo;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Repository implements IRepository {

    private Connection StartConnection() throws Exception {

        String url = "jdbc:postgresql://localhost:5432/SisComb";
        String username = "postgres";
        String password = "root";

        Connection connection = DriverManager.getConnection(url, username, password);

        return connection;
    }

    public void CreateDatabase() throws Exception {
        var connection = StartConnection();

        String sql = """
        
        CREATE TABLE IF NOT EXISTS public."Enderecos" (
            "Id" int GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            "UF" VARCHAR(2),
            "Cidade" VARCHAR(100),
            "Bairro" VARCHAR(100),
            "Rua" VARCHAR(100)
        );
        
        CREATE TABLE IF NOT EXISTS public."Usuarios" (
            "Id" int GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            "Nome" VARCHAR(255),
            "Email" VARCHAR(255),
            "EnderecoId" int,
            "TotalDenuncias" int,
        
            CONSTRAINT "FK_endereco_id" FOREIGN KEY ("EnderecoId")
            REFERENCES public."Enderecos" ("Id") MATCH SIMPLE
        );
        
        CREATE TABLE IF NOT EXISTS  public."Bandeiras" (
            "Id" int GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            "Nome" VARCHAR(255)
        );
        
        CREATE TABLE IF NOT EXISTS  public."Categorias" (
            "Id" int GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            "Nome" VARCHAR(255)
        );
        
        CREATE TABLE IF NOT EXISTS  public."Status" (
            "Id" int GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            "Nome" VARCHAR(255)
        );
      
        CREATE TABLE IF NOT EXISTS  public."Postos" (
            "Id" int GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            "RazaoSocial" VARCHAR(255),
            "CNPJ" VARCHAR(18),
            "EnderecoId" int,
            "BandeiraId" int,
        
            CONSTRAINT "FK_endereco_id" FOREIGN KEY ("EnderecoId")
            REFERENCES public."Enderecos" ("Id") MATCH SIMPLE,
        
            CONSTRAINT "FK_bandeira_id" FOREIGN KEY ("BandeiraId")
            REFERENCES public."Bandeiras" ("Id") MATCH SIMPLE
        );
        
        CREATE TABLE IF NOT EXISTS  public."Evidencias"(
            "Id" int GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            "Link" VARCHAR(300),
            "CriadoEm" TIMESTAMP DEFAULT NOW()
        );
        
        CREATE TABLE IF NOT EXISTS  public."Denuncias"(
            "Id" int GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            "UsuarioId" int,
            "PostoId" int,
            "CategoriaId" int,
            "Descricao" VARCHAR(500),
            "EvidenciaId" int,
            "StatusId" int,
            "CriadoEm" TIMESTAMP DEFAULT NOW(),
        
            CONSTRAINT "FK_usuario_id" FOREIGN KEY ("UsuarioId")
            REFERENCES public."Usuarios" ("Id") MATCH SIMPLE,
        
            CONSTRAINT "FK_posto_id" FOREIGN KEY ("PostoId")
            REFERENCES public."Postos" ("Id") MATCH SIMPLE,
        
            CONSTRAINT "FK_evidencia_id" FOREIGN KEY ("EvidenciaId")
            REFERENCES public."Evidencias" ("Id") MATCH SIMPLE,
        
            CONSTRAINT "FK_status_id" FOREIGN KEY ("StatusId")
            REFERENCES public."Status" ("Id") MATCH SIMPLE
        );
        
        """;

        var result =  connection.createStatement().execute(sql);

        System.out.println(result);
    }

    @Override
    public int GetLastId(String tableName) throws Exception {
        String url = "jdbc:postgresql://localhost:5432/SisComb";
        String username = "postgres";
        String password = "root";

        var connection = StartConnection();

        try (var statement = connection.createStatement()) {

            String sql = "SELECT MAX(\"Id\") as LastId FROM public.\"" + tableName + "\"";

            ResultSet resultSet = statement.executeQuery(sql);

            if (resultSet.next()) {
                return resultSet.getInt("LastId");
            }
        }

        return 0;
    }

    @Override
    public void CreateResumo() throws Exception {
        String sql = """
        
                        CREATE OR REPLACE VIEW public."vw_Resumo" AS
                                        WITH TopBandeira AS (
                                            SELECT\s
                                                b."Nome" AS Item,
                                                COUNT(d."Id") AS Quantidade,
                                                'Bandeira com mais denúncias' AS Categoria
                                            FROM public."Denuncias" d
                                            JOIN public."Postos" p ON d."PostoId" = p."Id"
                                            JOIN public."Bandeiras" b ON p."BandeiraId" = b."Id"
                                            GROUP BY b."Nome"
                                            ORDER BY Quantidade DESC
                                            LIMIT 1
                                        ),
                                        TopCidade AS (
                                            SELECT\s
                                                e."Cidade" AS Item,
                                                COUNT(d."Id") AS Quantidade,
                                                'Cidade com mais denúncias' AS Categoria
                                            FROM public."Denuncias" d
                                            JOIN public."Postos" p ON d."PostoId" = p."Id"
                                            JOIN public."Enderecos" e ON p."EnderecoId" = e."Id"
                                            GROUP BY e."Cidade"
                                            ORDER BY Quantidade DESC
                                            LIMIT 1
                                        )
                                        SELECT * FROM TopBandeira
                                        UNION ALL
                                        SELECT * FROM TopCidade;
        
        """;

        var connection = StartConnection();

        try (var statement = connection.prepareStatement(sql)) {

            var result = statement.execute();
        }
    }

    @Override
    public void SetupTrigger() throws Exception {
        var connection = StartConnection();

        String sqlCreateProcedure = """
                    CREATE OR REPLACE PROCEDURE public.InstalarGatilhosDenuncias()
                    LANGUAGE plpgsql
                    AS $$
                    BEGIN
                        EXECUTE '
                            CREATE OR REPLACE FUNCTION public.fn_incrementa_total_denuncias()
                            RETURNS TRIGGER AS $func$
                            BEGIN
                                UPDATE public."Usuarios"
                                SET "TotalDenuncias" = COALESCE("TotalDenuncias", 0) + 1
                                WHERE "Id" = NEW."UsuarioId";
                                RETURN NEW;
                            END;
                            $func$ LANGUAGE plpgsql;
                        ';
                
                        EXECUTE 'DROP TRIGGER IF EXISTS trg_atualiza_contador_denuncias ON public."Denuncias"';
                
                        EXECUTE '
                            CREATE TRIGGER trg_atualiza_contador_denuncias
                            AFTER INSERT ON public."Denuncias"
                            FOR EACH ROW
                            EXECUTE FUNCTION public.fn_incrementa_total_denuncias();
                        ';
                    END;
                    $$;
                """;

        try (Statement statement = connection.createStatement()) {
            statement.execute(sqlCreateProcedure);
            System.out.println("Procedure de instalação criada.");
        }

        try (CallableStatement cs = connection.prepareCall("CALL public.InstalarGatilhosDenuncias()")) {
            cs.execute();
            System.out.println("Trigger e Function instalados com sucesso.");
        }

        connection.close();
    }

    @Override
    public List<Resumo> GetResumo() throws Exception {

        String sql = """
        
        SELECT "item", "quantidade", "categoria" 
        FROM public."vw_Resumo"
        
        """;

        var connection = StartConnection();

        try (var statement = connection.prepareStatement(sql)) {

            var rs = statement.executeQuery();

            var lista = new ArrayList<Resumo>();

            while (rs.next()) {
                var resumo = new Resumo();

                resumo.item = rs.getString("Item");
                resumo.categoria = rs.getString("Categoria");
                //COUNT no Postgres retorna BigInt, por isso getLong
                resumo.quantidade = rs.getLong("Quantidade");

                lista.add(resumo);
            }

            return lista;
        }
    }

    @Override
    public boolean AddEndereco(Endereco endereco) throws Exception {

        String sql = """
        
        INSERT INTO public."Enderecos" (
            "UF",
            "Cidade",
            "Bairro",
            "Rua"
        ) VALUES (?, ?, ?, ?);
        
        """;

        var connection = StartConnection();

        try (var statement = connection.prepareStatement(sql)) {

            statement.setString(1, endereco.uf);
            statement.setString(2, endereco.cidade);
            statement.setString(3, endereco.bairro);
            statement.setString(4, endereco.rua);

            var result = statement.executeUpdate();

            return result == 1;
        }
    }

    @Override
    public boolean AddCategory(Categoria category) throws Exception {

        String sql = """
        
        INSERT INTO public."Categorias" (
            "Nome"
        ) VALUES (?);
        
        """;

        var connection = StartConnection();

        try (var statement = connection.prepareStatement(sql)) {

            statement.setString(1, category.nome);

            var result = statement.executeUpdate();

            return result == 1;
        }
    }

    @Override
    public boolean AddBandeira(Bandeira bandeira) throws Exception {

        String sql = """
        
        INSERT INTO public."Bandeiras" (
            "Nome"
        ) VALUES (?);
        
        """;

        var connection = StartConnection();

        try (var statement = connection.prepareStatement(sql)) {

            statement.setString(1, bandeira.nome);

            var result = statement.executeUpdate();

            return result == 1;
        }
    }

    @Override
    public boolean AddStatus(Status status) throws Exception {

        String sql = """
        
        INSERT INTO public."Status" (
            "Nome"
        ) VALUES (?);
        
        """;

        var connection = StartConnection();

        try (var statement = connection.prepareStatement(sql)) {

            statement.setString(1, status.nome);

            var result = statement.executeUpdate();

            return result == 1;
        }
    }


    @Override
    public boolean AddPosto(Posto posto) throws Exception {

        String sql = """
        
        INSERT INTO public."Postos" (
            "RazaoSocial",
            "CNPJ",
            "EnderecoId",
            "BandeiraId"
        ) VALUES (?, ?, ?, ?);
        
        """;

        var connection = StartConnection();

        try (var statement = connection.prepareStatement(sql)) {

            statement.setString(1, posto.razaoSocial);
            statement.setString(2, posto.cnpj);
            statement.setInt(3, posto.endereco.id);
            statement.setInt(4, posto.bandeira.id);

            var result = statement.executeUpdate();

            return result == 1;
        }
    }

    @Override
    public boolean AddEvidencia(Evidencia evidencia) throws Exception {

        String sql = """
        
        INSERT INTO public."Evidencias" (
            "Link"
        ) VALUES (?);
        
        """;

        var connection = StartConnection();

        try (var statement = connection.prepareStatement(sql)) {

            statement.setString(1, evidencia.link);

            var result = statement.executeUpdate();

            return result == 1;
        }
    }

    @Override
    public boolean AddDenuncia(Denuncia denuncia) throws Exception {

        String sql = """
    
    INSERT INTO public."Denuncias" (
        "Descricao",
        "UsuarioId",
        "StatusId",
        "PostoId",
        "CategoriaId",
        "EvidenciaId"
    ) VALUES (?, ?, ?, ?, ?, ?);
    
    """;

        var connection = StartConnection();

        try (var statement = connection.prepareStatement(sql)) {

            statement.setString(1, denuncia.descricao);
            statement.setInt(2, denuncia.usuarioId);
            statement.setInt(3, denuncia.statusId);
            statement.setInt(4, denuncia.postoId);
            statement.setInt(5, denuncia.categoriaId);
            statement.setInt(6, denuncia.evidenciaId);

            var result = statement.executeUpdate();

            return result == 1;
        }
    }

    @Override
    public boolean AddUsuario(Usuario usuario) throws Exception {

        String sql = """
    
    INSERT INTO public."Usuarios" (
        "Nome",
        "Email",
        "EnderecoId",
        "TotalDenuncias"
    ) VALUES (?, ?, ?, ?);
    
    """;

        var connection = StartConnection();

        try (var statement = connection.prepareStatement(sql)) {

            statement.setString(1, usuario.nome);
            statement.setString(2, usuario.email);

            statement.setInt(3, usuario.endereco.id);

            statement.setInt(4, usuario.TotalDenuncias);

            var result = statement.executeUpdate();

            return result == 1;
        }
    }

    // ---- ENDEREÇO
    @Override
    public Endereco GetEnderecoById(int id) throws Exception {
        String sql = """
    SELECT "Id", "UF", "Cidade", "Bairro", "Rua" 
    FROM public."Enderecos" 
    WHERE "Id" = ?
    """;

        var connection = StartConnection();
        try (var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            var resultSet = statement.executeQuery();

            if (resultSet.next()) {
                var endereco = new Endereco();
                endereco.id = resultSet.getInt("Id");
                endereco.uf = resultSet.getString("UF");
                endereco.cidade = resultSet.getString("Cidade");
                endereco.bairro = resultSet.getString("Bairro");
                endereco.rua = resultSet.getString("Rua");
                return endereco;
            }
            return null;
        }
    }

    @Override
    public boolean UpdateEndereco(Endereco endereco) throws Exception {
        String sql = """
    UPDATE public."Enderecos" 
    SET "UF" = ?, "Cidade" = ?, "Bairro" = ?, "Rua" = ?
    WHERE "Id" = ?
    """;

        var connection = StartConnection();
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, endereco.uf);
            statement.setString(2, endereco.cidade);
            statement.setString(3, endereco.bairro);
            statement.setString(4, endereco.rua);
            statement.setInt(5, endereco.id);

            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public boolean DeleteEndereco(int id) throws Exception {
        String sql = """
    DELETE FROM public."Enderecos" WHERE "Id" = ?
    """;

        var connection = StartConnection();
        try (var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() == 1;
        }
    }

    // CATEGORIA

    @Override
    public Categoria GetCategoriaById(int id) throws Exception {
        String sql = """
    SELECT "Id", "Nome" FROM public."Categorias" WHERE "Id" = ?
    """;
        var connection = StartConnection();
        try (var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            var rs = statement.executeQuery();
            if (rs.next()) {
                var cat = new Categoria();
                cat.id = rs.getInt("Id");
                cat.nome = rs.getString("Nome");
                return cat;
            }
            return null;
        }
    }

    @Override
    public boolean UpdateCategoria(Categoria categoria) throws Exception {
        String sql = """
    UPDATE public."Categorias" SET "Nome" = ? WHERE "Id" = ?
    """;
        var connection = StartConnection();
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, categoria.nome);
            statement.setInt(2, categoria.id);
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public boolean DeleteCategoria(int id) throws Exception {
        String sql = """
    DELETE FROM public."Categorias" WHERE "Id" = ?
    """;
        var connection = StartConnection();
        try (var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() == 1;
        }
    }

    //BANDEIRAS

    @Override
    public Bandeira GetBandeiraById(int id) throws Exception {
        String sql = """
    SELECT "Id", "Nome" FROM public."Bandeiras" WHERE "Id" = ?
    """;
        var connection = StartConnection();
        try (var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            var rs = statement.executeQuery();
            if (rs.next()) {
                var bandeira = new Bandeira();
                bandeira.id = rs.getInt("Id");
                bandeira.nome = rs.getString("Nome");
                return bandeira;
            }
            return null;
        }
    }

    @Override
    public boolean UpdateBandeira(Bandeira bandeira) throws Exception {
        String sql = """
    UPDATE public."Bandeiras" SET "Nome" = ? WHERE "Id" = ?
    """;
        var connection = StartConnection();
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, bandeira.nome);
            statement.setInt(2, bandeira.id);
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public boolean DeleteBandeira(int id) throws Exception {

        String sql = """
    
    DELETE FROM public."Bandeiras" 
    WHERE "Id" = ?
    
    """;

        var connection = StartConnection();

        try (var statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            var result = statement.executeUpdate();

            return result == 1;
        }
    }

    // STATUS

    @Override
    public Status GetStatusById(int id) throws Exception {

        String sql = """
    
    SELECT "Id", "Nome" 
    FROM public."Status" 
    WHERE "Id" = ?
    
    """;

        var connection = StartConnection();

        try (var statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            var resultSet = statement.executeQuery();

            if (resultSet.next()) {
                var status = new Status();
                status.id = resultSet.getInt("Id");
                status.nome = resultSet.getString("Nome");

                return status;
            }

            return null;
        }
    }

    @Override
    public boolean UpdateStatus(Status status) throws Exception {

        String sql = """
    
    UPDATE public."Status" 
    SET "Nome" = ?
    WHERE "Id" = ?
    
    """;

        var connection = StartConnection();

        try (var statement = connection.prepareStatement(sql)) {

            statement.setString(1, status.nome);
            statement.setInt(2, status.id);

            var result = statement.executeUpdate();

            return result == 1;
        }
    }

    @Override
    public boolean DeleteStatus(int id) throws Exception {

        String sql = """
    
    DELETE FROM public."Status" 
    WHERE "Id" = ?
    
    """;

        var connection = StartConnection();

        try (var statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            var result = statement.executeUpdate();

            return result == 1;
        }
    }


    //EVIDENCIA

    @Override
    public Evidencia GetEvidenciaById(int id) throws Exception {
        String sql = """
    SELECT "Id", "Link" FROM public."Evidencias" WHERE "Id" = ?
    """;
        var connection = StartConnection();
        try (var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            var rs = statement.executeQuery();
            if (rs.next()) {
                var ev = new Evidencia();
                ev.id = rs.getInt("Id");
                ev.link = rs.getString("Link");
                return ev;
            }
            return null;
        }
    }

    @Override
    public boolean UpdateEvidencia(Evidencia evidencia) throws Exception {
        String sql = """
    UPDATE public."Evidencias" SET "Link" = ? WHERE "Id" = ?
    """;
        var connection = StartConnection();
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, evidencia.link);
            statement.setInt(2, evidencia.id);
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public boolean DeleteEvidencia(int id) throws Exception {
        String sql = """
    DELETE FROM public."Evidencias" WHERE "Id" = ?
    """;
        var connection = StartConnection();
        try (var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() == 1;
        }
    }

    //USUARIO

    @Override
    public Usuario GetUsuarioById(int id) throws Exception {
        String sql = """
    SELECT u."Id", u."Nome", u."Email", u."TotalDenucias", 
           e."Id" as EndId, e."UF", e."Cidade", e."Bairro", e."Rua"
    FROM public."Usuarios" u
    INNER JOIN public."Enderecos" e ON u."EnderecoId" = e."Id"
    WHERE u."Id" = ?
    """;

        var connection = StartConnection();
        try (var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            var rs = statement.executeQuery();

            if (rs.next()) {
                // Reconstruindo o objeto Endereco primeiro
                var endereco = new Endereco();
                endereco.id = rs.getInt("EndId");
                endereco.uf = rs.getString("UF");
                endereco.cidade = rs.getString("Cidade");
                endereco.bairro = rs.getString("Bairro");
                endereco.rua = rs.getString("Rua");

                // Reconstruindo Usuario
                // Assumindo construtor: new Usuario(nome, email, endereco)
                var usuario = new Usuario(rs.getString("Nome"), rs.getString("Email"), endereco);
                usuario.id = rs.getInt("Id");
                usuario.TotalDenuncias = rs.getInt("TotalDenucias");

                return usuario;
            }
            return null;
        }
    }

    @Override
    public boolean UpdateUsuario(Usuario usuario) throws Exception {
        String sql = """
    UPDATE public."Usuarios" 
    SET "Nome" = ?, "Email" = ?, "EnderecoId" = ?, "TotalDenucias" = ?
    WHERE "Id" = ?
    """;

        var connection = StartConnection();
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, usuario.nome);
            statement.setString(2, usuario.email);
            statement.setInt(3, usuario.endereco.id); // Assume que o endereço existe
            statement.setInt(4, usuario.TotalDenuncias);
            statement.setInt(5, usuario.id);

            return statement.executeUpdate() == 1;
        }
    }

    public boolean DeleteUsuario(int id) throws Exception {
        String sql = """
    DELETE FROM public."Usuarios" WHERE "Id" = ?
    """;
        var connection = StartConnection();
        try (var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() == 1;
        }
    }

    // POSTO

    @Override
    public Posto GetPostoById(int id) throws Exception {
        String sql = """
    SELECT "Id", "RazaoSocial", "CNPJ", "EnderecoId", "BandeiraId"
    FROM public."Postos"
    WHERE "Id" = ?
    """;

        var connection = StartConnection();
        try (var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            var rs = statement.executeQuery();

            if (rs.next()) {
                var posto = new Posto();
                posto.id = rs.getInt("Id");
                posto.razaoSocial = rs.getString("RazaoSocial");
                posto.cnpj = rs.getString("CNPJ");

                // Instanciando objetos parciais apenas com ID para manter a referência
                posto.endereco = new Endereco();
                posto.endereco.id = rs.getInt("EnderecoId");

                posto.bandeira = new Bandeira();
                posto.bandeira.id = rs.getInt("BandeiraId");

                return posto;
            }
            return null;
        }
    }

    @Override
    public boolean UpdatePosto(Posto posto) throws Exception {
        String sql = """
    UPDATE public."Postos" 
    SET "RazaoSocial" = ?, "CNPJ" = ?, "EnderecoId" = ?, "BandeiraId" = ?
    WHERE "Id" = ?
    """;

        var connection = StartConnection();
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, posto.razaoSocial);
            statement.setString(2, posto.cnpj);
            statement.setInt(3, posto.endereco.id);
            statement.setInt(4, posto.bandeira.id);
            statement.setInt(5, posto.id);

            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public boolean DeletePosto(int id) throws Exception {
        String sql = """
    DELETE FROM public."Postos" WHERE "Id" = ?
    """;
        var connection = StartConnection();
        try (var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() == 1;
        }
    }

    //DENUNCIA

    @Override
    public Denuncia GetDenunciaById(int id) throws Exception {
        String sql = """
    SELECT "Id", "Descricao", "UsuarioId", "StatusId", "PostoId", "CategoriaId", "EvidenciaId"
    FROM public."Denuncias"
    WHERE "Id" = ?
    """;

        var connection = StartConnection();
        try (var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            var rs = statement.executeQuery();

            if (rs.next()) {
                var denuncia = new Denuncia();
                denuncia.id = rs.getInt("Id");
                denuncia.descricao = rs.getString("Descricao");
                denuncia.usuarioId = rs.getInt("UsuarioId");
                denuncia.statusId = rs.getInt("StatusId");
                denuncia.postoId = rs.getInt("PostoId");
                denuncia.categoriaId = rs.getInt("CategoriaId");
                denuncia.evidenciaId = rs.getInt("EvidenciaId");

                return denuncia;
            }
            return null;
        }
    }

    @Override
    public boolean UpdateDenuncia(Denuncia denuncia) throws Exception {
        String sql = """
    UPDATE public."Denuncias" 
    SET "Descricao" = ?, "UsuarioId" = ?, "StatusId" = ?, 
        "PostoId" = ?, "CategoriaId" = ?, "EvidenciaId" = ?
    WHERE "Id" = ?
    """;

        var connection = StartConnection();
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, denuncia.descricao);
            statement.setInt(2, denuncia.usuarioId);
            statement.setInt(3, denuncia.statusId);
            statement.setInt(4, denuncia.postoId);
            statement.setInt(5, denuncia.categoriaId);
            statement.setInt(6, denuncia.evidenciaId);
            statement.setInt(7, denuncia.id);

            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public boolean DeleteDenuncia(int id) throws Exception {
        String sql = """
    DELETE FROM public."Denuncias" WHERE "Id" = ?
    """;
        var connection = StartConnection();
        try (var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() == 1;
        }
    }
}
