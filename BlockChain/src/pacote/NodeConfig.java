package pacote;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuração lida da linha de comando.
 *
 * Uso:
 *   java Main --port 8333 --id NodeA --seed 192.168.1.10:8333 [--seed outro:8333] [--difficulty 4]
 * Atalhos: -p, -i, -s, -d. Também aceita posicional: Main <porta> <id> <seed>.
 */
public class NodeConfig {

    public static final int DEFAULT_PORT = 8333;
    public static final int DEFAULT_DIFFICULTY = 4;

    private final int port;
    private final String nodeId;
    private final List<String> seeds;
    private final int difficulty;

    private NodeConfig(int port, String nodeId, List<String> seeds, int difficulty) {
        this.port = port;
        this.nodeId = nodeId;
        this.seeds = seeds;
        this.difficulty = difficulty;
    }

    public int getPort() { return port; }
    public String getNodeId() { return nodeId; }
    /** Endereços "IP:Porta" dos nós semente (vazio se este nó é o primeiro da rede). */
    public List<String> getSeeds() { return seeds; }
    public int getDifficulty() { return difficulty; }

    public static NodeConfig parse(String[] args) {
        int port = DEFAULT_PORT;
        String id = null;
        int difficulty = DEFAULT_DIFFICULTY;
        List<String> seeds = new ArrayList<>();
        List<String> positional = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            switch (a) {
                case "--port": case "-p":
                    port = parsePort(value(args, ++i, a));
                    break;
                case "--id": case "-i":
                    id = value(args, ++i, a);
                    break;
                case "--seed": case "-s":
                    for (String s : value(args, ++i, a).split(",")) addSeed(seeds, s);
                    break;
                case "--difficulty": case "-d":
                    difficulty = parseDifficulty(value(args, ++i, a));
                    break;
                default:
                    if (a.startsWith("-")) throw new IllegalArgumentException("opção desconhecida: " + a);
                    positional.add(a);
            }
        }

        if (!positional.isEmpty()) {
            port = parsePort(positional.get(0));
            if (positional.size() > 1) id = positional.get(1);
            if (positional.size() > 2) addSeed(seeds, positional.get(2));
        }
        if (id == null || id.isBlank()) id = "Node-" + port;
        return new NodeConfig(port, id, seeds, difficulty);
    }

    public static String usage() {
        return "Uso: java Main [--port 8333] [--id NodeA] [--seed IP:PORTA]... [--difficulty 4]\n"
             + "  ou: java Main <porta> <id> <seed IP:PORTA>";
    }

    @Override
    public String toString() {
        return "porta=" + port + ", id=" + nodeId + ", seeds=" + seeds + ", dificuldade=" + difficulty;
    }

    // ------------------------------------------------------------------ helpers

    private static String value(String[] args, int i, String opt) {
        if (i >= args.length) throw new IllegalArgumentException("falta o valor de " + opt);
        return args[i];
    }

    private static int parsePort(String s) {
        try {
            int p = Integer.parseInt(s.trim());
            if (p < 1 || p > 65535) throw new NumberFormatException();
            return p;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("porta inválida: " + s);
        }
    }

    private static int parseDifficulty(String s) {
        try {
            int d = Integer.parseInt(s.trim());
            if (d < 0 || d > 8) throw new NumberFormatException();
            return d;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("dificuldade inválida (use 0 a 8): " + s);
        }
    }

    private static void addSeed(List<String> seeds, String raw) {
        String s = raw.trim();
        if (s.isEmpty()) return;
        int colon = s.lastIndexOf(':');
        if (colon <= 0 || colon == s.length() - 1) {
            throw new IllegalArgumentException("seed deve estar no formato IP:PORTA: " + s);
        }
        parsePort(s.substring(colon + 1));
        seeds.add(s);
    }
}
