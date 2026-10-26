package test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import pacote.Block;
import pacote.Blockchain;
import pacote.Menu;

/** Testes sem rede e sem dependências: java BlockchainTests */
public class BlockchainTests {

    private static int passed = 0, failed = 0;

    public static void main(String[] args) throws Exception {
        final int D = 2; // dificuldade baixa para os testes rodarem rápido

        check("gênese é igual em todos os nós",
                new Blockchain(D).getChainCopy().get(0).getHash()
                        .equals(new Blockchain(D).getChainCopy().get(0).getHash()));

        Blockchain a = new Blockchain(D);
        check("cadeia só com gênese é válida", a.ChainValida());

        a.mineBlock("b1"); a.mineBlock("b2"); a.mineBlock("b3");
        check("cadeia minerada tem 4 blocos", a.size() == 4);
        check("cadeia minerada é válida", a.ChainValida());
        check("blocos minerados respeitam a dificuldade",
                a.getChainCopy().stream().skip(1).allMatch(b -> b.getHash().startsWith("00")));

        // adulteração de dados
        Blockchain t = copyOf(a, D);
        t.corruptBlock(2, "hackeado");
        check("dados adulterados -> inválida", !t.ChainValida());

        // encadeamento quebrado (previousHash errado) com hash coerente
        List<Block> broken = a.getChainCopy();
        Block b2 = broken.get(2);
        Block fake = new Block(2, b2.getTimestamp(), b2.getData(), "abc");
        fake.minerar(D);
        broken.set(2, fake);
        check("encadeamento quebrado -> inválida", Blockchain.validar(broken, D) != null);

        // bloco sem PoW
        List<Block> noPow = a.getChainCopy();
        noPow.add(new Block(4, System.currentTimeMillis(), "sem PoW", a.getLast().getHash()));
        check("bloco não minerado -> inválida", Blockchain.validar(noPow, D) != null);

        // gênese diferente
        List<Block> otherGenesis = a.getChainCopy();
        otherGenesis.set(0, new Block(0, 1L, "outro gênese", "0"));
        check("gênese diferente -> inválida", Blockchain.validar(otherGenesis, D) != null);

        check("cadeia vazia/nula -> inválida",
                Blockchain.validar(new ArrayList<>(), D) != null && Blockchain.validar(null, D) != null);

        // consenso: maior cadeia válida
        Blockchain local = new Blockchain(D);
        local.mineBlock("x");
        Blockchain.ResultadoTroca r = local.tryReplace(a.getChainCopy());
        check("cadeia válida e maior -> aceita", r.aceita && local.size() == 4);

        Blockchain l2 = copyOf(a, D);
        r = l2.tryReplace(new Blockchain(D).getChainCopy());
        check("cadeia menor -> rejeitada", !r.aceita && r.motivo.contains("menor") && l2.size() == 4);

        r = l2.tryReplace(a.getChainCopy());
        check("cadeia de tamanho igual -> rejeitada", !r.aceita && l2.size() == 4);

        Blockchain l3 = new Blockchain(D);
        List<Block> bad = copyOf(a, D).getChainCopy(); // cópia profunda: não pode mexer nos blocos de 'a'
        bad.get(1).tamperData("fraude");
        r = l3.tryReplace(bad);
        check("cadeia maior porém adulterada -> rejeitada", !r.aceita && r.motivo.contains("inválida") && l3.size() == 1);

        // tryReplace copia a lista: mexer na original depois não afeta a cadeia local
        Blockchain l4 = new Blockchain(D);
        List<Block> src = a.getChainCopy();
        l4.tryReplace(src);
        src.clear();
        check("tryReplace faz cópia defensiva", l4.size() == 4);

        // serialização Java (payload de BLOCKCHAIN_COPY)
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) { oos.writeObject(a.getChainCopy()); }
        @SuppressWarnings("unchecked")
        ArrayList<Block> back = (ArrayList<Block>) new ObjectInputStream(
                new ByteArrayInputStream(bos.toByteArray())).readObject();
        check("serialização preserva a cadeia válida", Blockchain.validar(back, D) == null && back.size() == 4);

        // mineração descartada quando a ponta muda: simulado com tryReplace antes de adicionar
        // (mineBlock é síncrono aqui; basta garantir que blocos sucessivos se encadeiam)
        check("blocos sucessivos se encadeiam",
                a.getChainCopy().get(3).getPreviousHash().equals(a.getChainCopy().get(2).getHash()));

        // NodeConfig
        Menu.Config c = Menu.Config.parse(new String[]{"--port", "9000", "--id", "NodeB",
                "--seed", "192.168.1.10:8333,192.168.1.11:8333", "-d", "3"});
        check("args: porta/id/seeds/dificuldade", c.port == 9000 && c.nodeId.equals("NodeB")
                && c.seeds.size() == 2 && c.difficulty == 3);
        c = Menu.Config.parse(new String[]{"8334", "NodeC", "10.0.0.1:8333"});
        check("args posicionais", c.port == 8334 && c.nodeId.equals("NodeC")
                && c.seeds.get(0).equals("10.0.0.1:8333"));
        c = Menu.Config.parse(new String[]{});
        check("args padrão", c.port == 8333 && c.nodeId.equals("Node-8333") && c.seeds.isEmpty());
        check("seed inválida lança exceção", throwsIae(() -> Menu.Config.parse(new String[]{"--seed", "semporta"})));
        check("porta inválida lança exceção", throwsIae(() -> Menu.Config.parse(new String[]{"--port", "abc"})));

        // Menu com entrada roteirizada: minerar, corromper, mostrar, sair
        Blockchain mb = new Blockchain(D);
        int[] broadcasts = {0};
        Menu.NodeActions fake2 = new Menu.NodeActions() {
            public List<String> getPeers() { return List.of("192.168.1.11:8333"); }
            public void syncNow() {}
            public void broadcastChain() { broadcasts[0]++; }
        };
        String script = "1\nprimeiro\n1\nsegundo\n3\n5\n1\n2\n0\n";
        new Menu(mb, fake2, "NodeTest", new ByteArrayInputStream(script.getBytes())).run();
        check("menu: minerou 2 blocos e fez broadcast 2x", mb.size() == 3 && broadcasts[0] == 2);
        check("menu: corromper deixa a cadeia inválida", !mb.ChainValida());

        System.out.println("\n" + passed + " passaram, " + failed + " falharam.");
        if (failed > 0) System.exit(1);
    }

    private static Blockchain copyOf(Blockchain src, int d) {
        Blockchain c = new Blockchain(d);
        // reconstrói via serialização para obter blocos independentes
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(bos)) { oos.writeObject(src.getChainCopy()); }
            @SuppressWarnings("unchecked")
            ArrayList<Block> blocks = (ArrayList<Block>) new ObjectInputStream(
                    new ByteArrayInputStream(bos.toByteArray())).readObject();
            c.tryReplace(blocks);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return c;
    }

    private static boolean throwsIae(Runnable r) {
        try { r.run(); return false; } catch (IllegalArgumentException e) { return true; }
    }

    private static void check(String name, boolean ok) {
        if (ok) { passed++; System.out.println("  OK   " + name); }
        else { failed++; System.out.println("  FALHOU " + name); }
    }
}
