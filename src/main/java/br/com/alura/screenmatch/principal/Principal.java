package br.com.alura.screenmatch.principal;

import br.com.alura.screenmatch.models.*;
import br.com.alura.screenmatch.repository.SerieRepository;
import br.com.alura.screenmatch.service.ConsumoAPI;
import br.com.alura.screenmatch.service.ConverteDados;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {

    private Scanner leitura = new Scanner(System.in);
    private ConsumoAPI consumo = new ConsumoAPI();
    private ConverteDados conversor = new ConverteDados();
    private final String ENDERECO = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=6585022c";
    private SerieRepository repositorio;
    private List<Serie> series = new ArrayList<>();
    private List<DadosSerie> dadosSeries = new ArrayList<>();
    private Optional<Serie> serieBusca;

    public Principal(SerieRepository repositorio) {
        this.repositorio = repositorio;
    }

    public void exibeMenu() {
        var opcao = -1;
        while(opcao != 0) {
            var menu = """
                    1 - Buscar séries
                    2 - Buscar episódios
                    3 - Listar séries buscadas
                    4 - Buscar série por título
                    5 - Buscar séries pelo Ator
                    6 - Top 5 séries
                    7 - Buscar series por categoria
                    8 - Buscar series por Total de Temporadas e Avaliação
                    9 - Buscar episodio por trecho
                    10 - Top 5 episódios por série
                    11 - Buscar episódios a partir de uma data
                    
                    0 - Sair                                 
                    """;

            System.out.println(menu);
            opcao = leitura.nextInt();
            leitura.nextLine();

            switch (opcao) {
                case 1:
                    buscarSerieWeb();
                    break;
                case 2:
                    buscarEpisodioPorSerie();
                    break;
                case 3:
                    listarSeriesBuscadas();
                    break;
                case 4:
                    buscarSeriePorTitulo();
                    break;
                case 5:
                    buscarSeriesPorAtor();
                    break;
                case 6:
                    buscarTop5Series();
                    break;
                case 7:
                    buscarSeriesPorCategoria();
                    break;
                case 8:
                    buscarSeriePorTotalDeTemporadas();
                    break;
                case 9:
                    buscarEpisodioPorTrecho();
                    break;
                case 10:
                    topEpisodiosPorSerie();
                    break;
                case 11:
                    buscarEpisodiosDepoisDeUmaData();
                    break;
                case 0:
                    System.out.println("Saindo...");
                    break;
                default:
                    System.out.println("Opção inválida");
            }
        }
    }

    private void buscarSerieWeb() {
        DadosSerie dados = getDadosSerie();
        Serie serie = new Serie(dados);
        repositorio.save(serie);
        System.out.println(dados);
    }

    private DadosSerie getDadosSerie() {
        System.out.println("Digite o nome da série para busca");
        var nomeSerie = leitura.nextLine();
        var json = consumo.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + API_KEY);
        DadosSerie dados = conversor.obterDados(json, DadosSerie.class);
        return dados;
    }

    private void buscarEpisodioPorSerie(){
        listarSeriesBuscadas();
        System.out.print("Escolha uma Série pelo nome: ");
        var nomeSerie = leitura.nextLine();
        Optional<Serie> serie = repositorio.findByTituloContainingIgnoreCase(nomeSerie);

        if(serie.isPresent()){
            var serieEncontrada = serie.get();
            List<DadosTemporada> temporadas = new ArrayList<>();

            for (int i = 1; i <= serieEncontrada.getTotalTemporadas(); i++) {
                var json = consumo.obterDados(ENDERECO + serieEncontrada.getTitulo()
                        .replace(" ", "+") + "&season=" + i + API_KEY);
                DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
                temporadas.add(dadosTemporada);
            }
            temporadas.forEach(System.out::println);

            List<Episodio> episodios = temporadas.stream()
                    .flatMap(dadosTemporada -> dadosTemporada.episodios().stream()
                            .map(dadosEpisodio -> new Episodio(dadosTemporada.numero(), dadosEpisodio)))
                    .collect(Collectors.toList());
            serieEncontrada.setEpisodios(episodios);
            repositorio.save(serieEncontrada);
        }else{
            System.out.println("Serie não encontrada!");
        }
    }

    private void listarSeriesBuscadas(){
        series = repositorio.findAll();
        series.stream()
                        .sorted(Comparator.comparing(Serie::getGenero))
                                .forEach(System.out::println);
        dadosSeries.forEach(System.out::println);
    }

    private void buscarSeriePorTitulo() {
        System.out.print("Escolha uma Série pelo nome: ");
        var nomeSerie = leitura.nextLine();
        serieBusca = repositorio.findByTituloContainingIgnoreCase(nomeSerie);

        if(serieBusca.isPresent()){
            System.out.println("Dados da série: " + serieBusca.get());
        }else{
            System.out.println("Série não encontrada!");
        }
    }

    private void buscarSeriesPorAtor() {
        System.out.println("Qual o nome para busca?");
        var nomeAtor = leitura.nextLine();
        System.out.println("Avaliações a partir de que valor?");
        var avaliacao = leitura.nextDouble();
        List<Serie> seriesEncontradas = repositorio.findByAtoresContainingIgnoreCaseAndAvaliacaoGreaterThanEqual(nomeAtor, avaliacao);
        System.out.println("Séries em que " + nomeAtor + " trabalhou: ");
        seriesEncontradas.forEach( serie -> System.out.println(serie.getTitulo() + " avaliação: " + serie.getAvaliacao()));
    }

    private void buscarTop5Series() {
        List<Serie> serieTop = repositorio.findTop5ByOrderByAvaliacaoDesc();
        serieTop.forEach(serie -> System.out.println(serie.getTitulo() + " avaliação: " + serie.getAvaliacao()));
    }

    private void buscarSeriesPorCategoria() {
        System.out.println("Deseja buscar series de qual categoria/gênero?");
        var nomeGenero = leitura.nextLine();
        Categoria categoria = Categoria.fromPortugues(nomeGenero);
        List<Serie> seriesPorCategoria = repositorio.findByGenero(categoria);
        seriesPorCategoria.forEach(System.out::println);
    }

    private void buscarSeriePorTotalDeTemporadas() {
        System.out.print("Digite o total de temporadas: ");
        var totalTemporadas = leitura.nextInt();
        leitura.nextLine();
        System.out.print("Digite a avaliação: ");
        var avaliacao = leitura.nextDouble();
        leitura.nextLine();
        List<Serie> seriesPorTotalTemporada = repositorio
                .seriesPorTemporadaEAvaliacao(totalTemporadas, avaliacao);
        seriesPorTotalTemporada.forEach(serie -> System.out.println(serie.getTitulo() + " - avaliação: " + serie.getAvaliacao()));
    }

    private void buscarEpisodioPorTrecho(){
        System.out.print("Digite o trecho do episódio: ");
        var trechoEpisodio = leitura.nextLine();
        List<Episodio> episodiosEncontrados = repositorio.episodiosPorTrecho(trechoEpisodio);
        episodiosEncontrados.forEach(episodio ->
                System.out.printf("Série: %s Temporada: %d - Episódio: %d - %s \n",
                        episodio.getSerie().getTitulo(), episodio.getTemporada(),
                        episodio.getNumero(), episodio.getTitulo()));
    }

    private void topEpisodiosPorSerie(){
        buscarSeriePorTitulo();

        if(serieBusca.isPresent()){
            Serie serie = serieBusca.get();
            List<Episodio> topEpisodios = repositorio.topEpisodiosPorSerie(serie);
            topEpisodios.forEach(episodio ->
                    System.out.printf("Série: %s Temporada: %d - Episódio: %d - %s Avaliação: %s\n",
                            episodio.getSerie().getTitulo(), episodio.getTemporada(),
                            episodio.getNumero(), episodio.getTitulo(), episodio.getAvaliacao()));
        }
    }

    private void buscarEpisodiosDepoisDeUmaData(){
        buscarSeriePorTitulo();

        if(serieBusca.isPresent()){
            System.out.print("Digite o ano limite de lançamento: ");
            var anoLancamento = leitura.nextInt();
            leitura.nextLine();
            Serie serie = serieBusca.get();

            List<Episodio> episodiosAno = repositorio.episodiosPorSerieEAno(serie, anoLancamento);
            episodiosAno.forEach(System.out::println);

        }
    }
}