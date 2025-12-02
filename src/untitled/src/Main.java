import Entities.*;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws Exception {
        IRepository repository = new Repository();

        System.out.println("--- INICIANDO SETUP ---");
        System.out.println();
        System.out.println("--- CRIANDO BANCO DE DADOS ---");
        repository.CreateDatabase();

        System.out.println("--- CONFIGURANDO FUNCTION E GATILHO DA FUNCTION ---");
        repository.SetupTrigger();

        // ==================================================================================
        // 1. CRIAÇÃO DE ENTIDADES INDEPENDENTES E CAPTURA DE IDs
        // ==================================================================================
        System.out.println("\n--- 1. INSERINDO DADOS BASE ---");

        // --- Endereco ---
        Endereco endereco = new Endereco();
        endereco.uf = "SP";
        endereco.cidade = "São Paulo";
        endereco.bairro = "Morumbi";
        endereco.rua = "Av. Principal";
        repository.AddEndereco(endereco);

        // RECUPERA O ID GERADO
        int enderecoId = repository.GetLastId("Enderecos");
        endereco.id = enderecoId;
        System.out.println("Endereco criado com ID: " + enderecoId);

        // --- Categoria ---
        Categoria categoria = new Categoria();
        categoria.nome = "Combustível Adulterado";
        repository.AddCategory(categoria);
        int categoriaId = repository.GetLastId("Categorias");
        System.out.println("Categoria criado com ID: " + categoriaId);

        // --- Bandeira ---
        Bandeira bandeira = new Bandeira();
        bandeira.nome = "Shell";
        repository.AddBandeira(bandeira);
        int bandeiraId = repository.GetLastId("Bandeiras");
        System.out.println("Bandeira criado com ID: " + bandeiraId);

        // --- Status ---
        Status status = new Status();
        status.nome = "Em Análise";
        repository.AddStatus(status);
        int statusId = repository.GetLastId("Status");
        System.out.println("Status criado com ID: " + statusId);

        // --- Evidencia ---
        Evidencia evidencia = new Evidencia();
        evidencia.link = "http://foto-comprovante.jpg";
        repository.AddEvidencia(evidencia);
        int evidenciaId = repository.GetLastId("Evidencias");
        System.out.println("Evidencia criado com ID: " + evidenciaId);


        // ==================================================================================
        // 2. CRIAÇÃO DE ENTIDADES DEPENDENTES (Usando os IDs capturados)
        // ==================================================================================
        System.out.println("\n--- 2. INSERINDO DEPENDENTES ---");

        // --- Usuario ---
        // Precisamos passar o objeto Endereco já com o ID correto que setamos acima
        Usuario usuario = new Usuario("Maria Teste", "maria@teste.com", endereco);
        repository.AddUsuario(usuario);

        int usuarioId = repository.GetLastId("Usuarios");
        System.out.println("Usuario criado com ID: " + usuarioId);

        // --- Posto ---
        Posto posto = new Posto();
        posto.razaoSocial = "Posto da Esquina LTDA";
        posto.cnpj = "99.999.999/0001-99";

        // Criando objetos auxiliares apenas com o ID para salvar a FK
        posto.endereco = new Endereco();
        posto.endereco.id = enderecoId; // Usa o ID capturado do endereço

        posto.bandeira = new Bandeira();
        posto.bandeira.id = bandeiraId; // Usa o ID capturado da bandeira

        repository.AddPosto(posto);
        int postoId = repository.GetLastId("Postos");
        System.out.println("Posto criado com ID: " + postoId);


        // ==================================================================================
        // 3. CRIAÇÃO DA ENTIDADE PRINCIPAL (Denuncia)
        // ==================================================================================
        System.out.println("\n--- 3. INSERINDO DENÚNCIA ---");

        Denuncia denuncia = new Denuncia();
        denuncia.descricao = "Suspeita de bomba fraudada.";

        denuncia.usuarioId = usuarioId;
        denuncia.postoId = postoId;
        denuncia.categoriaId = categoriaId;
        denuncia.statusId = statusId;
        denuncia.evidenciaId = evidenciaId;

        if(repository.AddDenuncia(denuncia)) {
            System.out.println("[SUCESSO] Denuncia Adicionada vinculada ao Usuario " + usuarioId + " e Posto " + postoId);
        } else {
            System.out.println("[ERRO] Falha ao adicionar Denuncia");
        }

        int denunciaId = repository.GetLastId("Denuncias");


        // ==================================================================================
        // 4. TESTE DE LEITURA
        // ==================================================================================
        System.out.println("\n--- 4. LEITURA ---");

        Denuncia denunciaExistente = repository.GetDenunciaById(denunciaId);
        if(denunciaExistente != null) {
            System.out.println("Denuncia lida do banco: " + denunciaExistente.descricao);
            System.out.println("FK Usuario ID: " + denunciaExistente.usuarioId);
        }

        // ==================================================================================
        // 5. ATUALIZAÇÃO
        // ==================================================================================
        System.out.println("\n--- 5. ATUALIZAÇÃO ---");

        if(denunciaExistente != null) {
            System.out.println("Descrição da Denuncia Antes de ser atualizada: " + denunciaExistente.descricao);

            denunciaExistente.descricao = "Descrição alterada pelo teste.";
            repository.UpdateDenuncia(denunciaExistente);
            System.out.println("Denuncia atualizada.");

            Denuncia denunciaAtualizada = repository.GetDenunciaById(denunciaId);
            if(denunciaAtualizada != null) {
                System.out.println("Denuncia com a descrição atualizada: " + denunciaAtualizada.descricao);
            }
        }

        // ==================================================================================
        // 6. RESUMO utilizando View
        // ==================================================================================
        System.out.println("\n--- 5. RESUMO ---");

        repository.CreateResumo();
        var resumos = repository.GetResumo();

        for (var resumo : resumos) {
            System.out.println(resumo.categoria + ": " + resumo.item + " (" + resumo.quantidade + " denúncias)");
        }




        // ==================================================================================
        // 7. EXCLUSÃO (Em ordem inversa para não ter FOREIGN KEY VIOLATION)
        // ==================================================================================
        System.out.println("\n--- 6. EXCLUSÃO ---");

        // 1. Filhos (Denuncia depende de todos)
        repository.DeleteDenuncia(denunciaId);
        System.out.println("Denuncia deletada.");

        // 2. Nível Intermediário
        repository.DeletePosto(postoId);
        repository.DeleteUsuario(usuarioId);
        System.out.println("Posto e Usuário deletados.");
        System.out.println("\n");

        // 3. Pais (Independentes)
        repository.DeleteEvidencia(evidenciaId);
        System.out.println("Evidência deletada.");

        repository.DeleteStatus(statusId);
        System.out.println("Status deletada.");

        repository.DeleteCategoria(categoriaId);
        System.out.println("Categoria deletada.");

        repository.DeleteBandeira(bandeiraId); // Posto depende desta
        System.out.println("Bandeira deletada.");

        repository.DeleteEndereco(enderecoId); // Usuario e Posto dependem deste
        System.out.println("Endereco deletada.");


        System.out.println("Limpeza completa.");
    }
}