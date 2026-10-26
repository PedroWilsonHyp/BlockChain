package pacote;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Log legível para a apresentação. Todos os integrantes do grupo podem usar. */
public final class Log {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static volatile String nodeId = "?";

    private Log() {}

    public static void setNodeId(String id) { nodeId = id; }

    public static void info(String fmt, Object... args) { print("INFO ", fmt, args); }

    public static void warn(String fmt, Object... args) { print("AVISO", fmt, args); }

    public static void error(String fmt, Object... args) { print("ERRO ", fmt, args); }

    /** Ex.: "recebi BLOCKCHAIN_COPY de 192.168.1.11:8333, tamanho 5, aceita". */
    public static void received(String type, String from, String detail) {
        info("recebi %s de %s, %s", type, from, detail);
    }

    public static void sent(String type, String to, String detail) {
        info("enviei %s para %s, %s", type, to, detail);
    }

    private static synchronized void print(String level, String fmt, Object[] args) {
        String msg = (args == null || args.length == 0) ? fmt : String.format(fmt, args);
        System.out.println("[" + LocalTime.now().format(FMT) + "] [" + nodeId + "] " + level + " " + msg);
    }
}
