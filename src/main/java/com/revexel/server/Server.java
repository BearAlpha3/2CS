package com.revexel.server;

import com.sun.net.httpserver.HttpServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Server extends WebSocketServer {

    private static final int DEFAULT_PORT = 10000;

    private final Database database;

    private final Map<WebSocket, PlayerData> onlinePlayers =
        new ConcurrentHashMap<WebSocket, PlayerData>();

    private final Map<String, WebSocket> playersById =
        new ConcurrentHashMap<String, WebSocket>();

    public Server(int port) {

        super(
            new InetSocketAddress(
                "0.0.0.0",
                port
            )
        );

        database =
            new Database();
    }

    @Override
    public void onOpen(
        WebSocket connection,
        ClientHandshake handshake
    ) {

        System.out.println(
            "[CONNECT] " +
            connection.getRemoteSocketAddress()
        );

        JSONObject response =
            new JSONObject();

        response.put(
            "type",
            "connected"
        );

        response.put(
            "server",
            "Revexel"
        );

        response.put(
            "version",
            "1.0"
        );

        send(
            connection,
            response
        );
    }

    @Override
    public void onClose(
        WebSocket connection,
        int code,
        String reason,
        boolean remote
    ) {

        PlayerData player =
            onlinePlayers.remove(
                connection
            );

        if (player != null) {

            playersById.remove(
                player.playerId
            );

            database.savePlayer(
                player
            );

            JSONObject response =
                new JSONObject();

            response.put(
                "type",
                "player_leave"
            );

            response.put(
                "playerId",
                player.playerId
            );

            broadcast(
                response
            );

            System.out.println(
                "[DISCONNECT] " +
                player.nick +
                " (" +
                player.playerId +
                ")"
            );
        }
    }

    @Override
    public void onMessage(
        WebSocket connection,
        String message
    ) {

        try {

            JSONObject data =
                new JSONObject(message);

            String type =
                data.optString(
                    "type",
                    ""
                );

            switch (type) {

                case "create_account":

                    createAccount(
                        connection
                    );

                    break;

                case "login":

                    login(
                        connection,
                        data
                    );

                    break;

                case "change_nick":

                    changeNick(
                        connection,
                        data
                    );

                    break;

                case "save_player":

                    savePlayer(
                        connection,
                        data
                    );

                    break;

                case "load_player":

                    loadPlayer(
                        connection,
                        data
                    );

                    break;

                case "move":

                    movePlayer(
                        connection,
                        data
                    );

                    break;

                case "ping":

                    JSONObject pong =
                        new JSONObject();

                    pong.put(
                        "type",
                        "pong"
                    );

                    send(
                        connection,
                        pong
                    );

                    break;

                default:

                    sendError(
                        connection,
                        "unknown_command",
                        "Comando desconhecido."
                    );

                    break;
            }

        } catch (Exception e) {

            System.out.println(
                "[MESSAGE ERROR] " +
                e.getMessage()
            );

            sendError(
                connection,
                "invalid_message",
                "Mensagem inválida."
            );
        }
    }

    private void createAccount(
        WebSocket connection
    ) {

        PlayerData player =
            database.createPlayer();

        onlinePlayers.put(
            connection,
            player
        );

        playersById.put(
            player.playerId,
            connection
        );

        JSONObject response =
            new JSONObject();

        response.put(
            "type",
            "account_created"
        );

        response.put(
            "success",
            true
        );

        response.put(
            "playerId",
            player.playerId
        );

        response.put(
            "nick",
            player.nick
        );

        response.put(
            "level",
            player.level
        );

        response.put(
            "xp",
            player.xp
        );

        response.put(
            "money",
            player.money
        );

        response.put(
            "gems",
            player.gems
        );

        send(
            connection,
            response
        );

        System.out.println(
            "[ACCOUNT CREATED] " +
            player.playerId
        );
    }

    private void login(
        WebSocket connection,
        JSONObject data
    ) {

        String playerId =
            data.optString(
                "playerId",
                ""
            ).trim();

        if (playerId.isEmpty()) {

            sendError(
                connection,
                "missing_player_id",
                "PlayerID não informado."
            );

            return;
        }

        PlayerData player =
            database.getPlayer(
                playerId
            );

        if (player == null) {

            sendError(
                connection,
                "account_not_found",
                "Conta não encontrada."
            );

            return;
        }

        WebSocket oldConnection =
            playersById.get(
                player.playerId
            );

        if (
            oldConnection != null &&
            oldConnection != connection
        ) {

            try {

                JSONObject kicked =
                    new JSONObject();

                kicked.put(
                    "type",
                    "session_replaced"
                );

                kicked.put(
                    "message",
                    "Sua conta entrou em outro dispositivo."
                );

                oldConnection.send(
                    kicked.toString()
                );

                oldConnection.close();

            } catch (Exception ignored) {
            }
        }

        onlinePlayers.put(
            connection,
            player
        );

        playersById.put(
            player.playerId,
            connection
        );

        JSONObject response =
            new JSONObject();

        response.put(
            "type",
            "login_success"
        );

        response.put(
            "success",
            true
        );

        response.put(
            "playerId",
            player.playerId
        );

        response.put(
            "player",
            player.toJSON()
        );

        send(
            connection,
            response
        );

        System.out.println(
            "[LOGIN] " +
            player.nick +
            " (" +
            player.playerId +
            ")"
        );
    }

    private void changeNick(
        WebSocket connection,
        JSONObject data
    ) {

        PlayerData player =
            onlinePlayers.get(
                connection
            );

        if (player == null) {

            sendError(
                connection,
                "not_logged",
                "Você não está conectado à conta."
            );

            return;
        }

        String nick =
            data.optString(
                "nick",
                ""
            ).trim();

        if (
            nick.length() < 4
        ) {

            sendError(
                connection,
                "invalid_nick",
                "O Nick precisa ter pelo menos 4 caracteres."
            );

            return;
        }

        if (
            nick.length() > 16
        ) {

            sendError(
                connection,
                "invalid_nick",
                "O Nick pode ter no máximo 16 caracteres."
            );

            return;
        }

        if (
            !nick.matches(
                "[a-zA-Z0-9_]+"
            )
        ) {

            sendError(
                connection,
                "invalid_nick",
                "Use apenas letras, números e _."
            );

            return;
        }

        if (
            database.nickExists(nick) &&
            !player.nick.equalsIgnoreCase(nick)
        ) {

            sendError(
                connection,
                "nick_taken",
                "Esse Nick já está sendo usado."
            );

            return;
        }

        player.nick =
            nick;

        database.savePlayer(
            player
        );

        JSONObject response =
            new JSONObject();

        response.put(
            "type",
            "nick_changed"
        );

        response.put(
            "success",
            true
        );

        response.put(
            "playerId",
            player.playerId
        );

        response.put(
            "nick",
            player.nick
        );

        send(
            connection,
            response
        );

        JSONObject broadcastData =
            new JSONObject();

        broadcastData.put(
            "type",
            "player_update"
        );

        broadcastData.put(
            "playerId",
            player.playerId
        );

        broadcastData.put(
            "nick",
            player.nick
        );

        broadcast(
            broadcastData
        );

        System.out.println(
            "[NICK] " +
            player.playerId +
            " -> " +
            player.nick
        );
    }

    private void savePlayer(
        WebSocket connection,
        JSONObject data
    ) {

        PlayerData player =
            onlinePlayers.get(
                connection
            );

        if (player == null) {

            sendError(
                connection,
                "not_logged",
                "Conta não conectada."
            );

            return;
        }

        if (
            data.has("x")
        ) {

            player.x =
                data.optDouble(
                    "x",
                    player.x
                );
        }

        if (
            data.has("y")
        ) {

            player.y =
                data.optDouble(
                    "y",
                    player.y
                );
        }

        if (
            data.has("level")
        ) {

            player.level =
                data.optInt(
                    "level",
                    player.level
                );
        }

        if (
            data.has("xp")
        ) {

            player.xp =
                data.optLong(
                    "xp",
                    player.xp
                );
        }

        if (
            data.has("money")
        ) {

            player.money =
                data.optLong(
                    "money",
                    player.money
                );
        }

        if (
            data.has("gems")
        ) {

            player.gems =
                data.optLong(
                    "gems",
                    player.gems
                );
        }

        if (
            data.has("fightingStyle")
        ) {

            player.fightingStyle =
                data.optString(
                    "fightingStyle",
                    player.fightingStyle
                );
        }

        if (
            data.has("inventory")
        ) {

            JSONObject inventory =
                data.optJSONObject(
                    "inventory"
                );

            if (inventory != null) {
                player.inventory =
                    inventory;
            }
        }

        if (
            data.has("pets")
        ) {

            JSONObject pets =
                data.optJSONObject(
                    "pets"
                );

            if (pets != null) {
                player.pets =
                    pets;
            }
        }

        if (
            data.has("powers")
        ) {

            JSONObject powers =
                data.optJSONObject(
                    "powers"
                );

            if (powers != null) {
                player.powers =
                    powers;
            }
        }

        if (
            data.has("stats")
        ) {

            JSONObject stats =
                data.optJSONObject(
                    "stats"
                );

            if (stats != null) {
                player.stats =
                    stats;
            }
        }

        database.savePlayer(
            player
        );

        JSONObject response =
            new JSONObject();

        response.put(
            "type",
            "save_success"
        );

        response.put(
            "playerId",
            player.playerId
        );

        send(
            connection,
            response
        );
    }

    private void loadPlayer(
        WebSocket connection,
        JSONObject data
    ) {

        PlayerData player =
            onlinePlayers.get(
                connection
            );

        if (player == null) {

            String playerId =
                data.optString(
                    "playerId",
                    ""
                );

            player =
                database.getPlayer(
                    playerId
                );

            if (player == null) {

                sendError(
                    connection,
                    "account_not_found",
                    "Conta não encontrada."
                );

                return;
            }

            onlinePlayers.put(
                connection,
                player
            );

            playersById.put(
                player.playerId,
                connection
            );
        }

        JSONObject response =
            new JSONObject();

        response.put(
            "type",
            "player_data"
        );

        response.put(
            "player",
            player.toJSON()
        );

        send(
            connection,
            response
        );
    }

    private void movePlayer(
        WebSocket connection,
        JSONObject data
    ) {

        PlayerData player =
            onlinePlayers.get(
                connection
            );

        if (player == null) {

            sendError(
                connection,
                "not_logged",
                "Conta não conectada."
            );

            return;
        }

        double x =
            data.optDouble(
                "x",
                player.x
            );

        double y =
            data.optDouble(
                "y",
                player.y
            );

        if (
            Double.isNaN(x) ||
            Double.isInfinite(x) ||
            Double.isNaN(y) ||
            Double.isInfinite(y)
        ) {
            return;
        }

        x =
            Math.max(
                -100000,
                Math.min(
                    100000,
                    x
                )
            );

        y =
            Math.max(
                -100000,
                Math.min(
                    100000,
                    y
                )
            );

        player.x = x;
        player.y = y;

        JSONObject response =
            new JSONObject();

        response.put(
            "type",
            "player_move"
        );

        response.put(
            "playerId",
            player.playerId
        );

        response.put(
            "nick",
            player.nick
        );

        response.put(
            "x",
            player.x
        );

        response.put(
            "y",
            player.y
        );

        broadcast(
            response
        );
    }

    private void sendError(
        WebSocket connection,
        String code,
        String message
    ) {

        JSONObject response =
            new JSONObject();

        response.put(
            "type",
            "error"
        );

        response.put(
            "code",
            code
        );

        response.put(
            "message",
            message
        );

        send(
            connection,
            response
        );
    }

    private void send(
        WebSocket connection,
        JSONObject data
    ) {

        if (
            connection != null &&
            connection.isOpen()
        ) {

            connection.send(
                data.toString()
            );
        }
    }

    private void broadcast(
        JSONObject data
    ) {

        broadcast(
            data.toString()
        );
    }

    @Override
    public void onError(
        WebSocket connection,
        Exception exception
    ) {

        System.out.println(
            "[ERROR] " +
            exception.getMessage()
        );
    }

    @Override
    public void onStart() {

        System.out.println(
            "REVEXEL SERVER ONLINE"
        );

        System.out.println(
            "PORT: " +
            getPort()
        );

        System.out.println(
            "DATABASE: players.json"
        );
    }

    public static void main(
        String[] args
    ) {

        int port =
            DEFAULT_PORT;

        String envPort =
            System.getenv("PORT");

        if (
            envPort != null &&
            !envPort.trim().isEmpty()
        ) {

            try {

                port =
                    Integer.parseInt(
                        envPort
                    );

            } catch (Exception ignored) {
            }
        }

        Server server =
            new Server(port);

        server.start();

        System.out.println(
            "Starting Revexel Server..."
        );
    }
              }
