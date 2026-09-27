package com.revexel.server;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public class Database {

    private static final String FILE_NAME =
        "players.json";

    private JSONObject database;

    public Database() {

        database =
            new JSONObject();

        load();
    }

    private synchronized void load() {

        try {

            File file =
                new File(FILE_NAME);

            if (!file.exists()) {
                save();
                return;
            }

            FileInputStream input =
                new FileInputStream(file);

            byte[] bytes =
                new byte[(int) file.length()];

            input.read(bytes);
            input.close();

            String text =
                new String(
                    bytes,
                    StandardCharsets.UTF_8
                );

            if (!text.trim().isEmpty()) {

                database =
                    new JSONObject(text);
            }

        } catch (Exception e) {

            System.out.println(
                "[DATABASE] Erro ao carregar: " +
                e.getMessage()
            );

            database =
                new JSONObject();
        }
    }

    private synchronized void save() {

        try {

            FileOutputStream output =
                new FileOutputStream(FILE_NAME);

            output.write(
                database
                    .toString(2)
                    .getBytes(
                        StandardCharsets.UTF_8
                    )
            );

            output.flush();
            output.close();

        } catch (Exception e) {

            System.out.println(
                "[DATABASE] Erro ao salvar: " +
                e.getMessage()
            );
        }
    }

    public synchronized PlayerData createPlayer() {

        String id;

        do {

            id =
                "RVX-" +
                java.util.UUID
                    .randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 8)
                    .toUpperCase();

        } while (
            database.has(id)
        );

        PlayerData player =
            new PlayerData(id);

        database.put(
            id,
            player.toJSON()
        );

        save();

        return player;
    }

    public synchronized PlayerData getPlayer(
        String id
    ) {

        if (!database.has(id)) {
            return null;
        }

        JSONObject data =
            database.optJSONObject(id);

        if (data == null) {
            return null;
        }

        return PlayerData.fromJSON(data);
    }

    public synchronized boolean savePlayer(
        PlayerData player
    ) {

        if (player == null) {
            return false;
        }

        database.put(
            player.playerId,
            player.toJSON()
        );

        save();

        return true;
    }

    public synchronized boolean nickExists(
        String nick
    ) {

        String wanted =
            nick.toLowerCase();

        java.util.Iterator<String> keys =
            database.keys();

        while (keys.hasNext()) {

            String id =
                keys.next();

            JSONObject data =
                database.optJSONObject(id);

            if (data == null) {
                continue;
            }

            String current =
                data.optString(
                    "nick",
                    ""
                );

            if (
                current.equalsIgnoreCase(
                    wanted
                )
            ) {
                return true;
            }
        }

        return false;
    }
              }
