package com.revexel.server;

import org.json.JSONObject;

public class PlayerData {

    public String playerId;
    public String nick;

    public int level;
    public long xp;
    public long money;
    public long gems;

    public double x;
    public double y;

    public int rank;
    public int adminLevel;

    public String fightingStyle;

    public JSONObject inventory;
    public JSONObject pets;
    public JSONObject powers;
    public JSONObject stats;

    public PlayerData(String playerId) {
        this.playerId = playerId;

        this.nick = "";

        this.level = 1;
        this.xp = 0;
        this.money = 0;
        this.gems = 0;

        this.x = 0;
        this.y = 0;

        this.rank = 0;
        this.adminLevel = 0;

        this.fightingStyle = "Normal";

        this.inventory = new JSONObject();
        this.pets = new JSONObject();
        this.powers = new JSONObject();
        this.stats = new JSONObject();
    }

    public JSONObject toJSON() {

        JSONObject data = new JSONObject();

        data.put("playerId", playerId);
        data.put("nick", nick);

        data.put("level", level);
        data.put("xp", xp);
        data.put("money", money);
        data.put("gems", gems);

        data.put("x", x);
        data.put("y", y);

        data.put("rank", rank);
        data.put("adminLevel", adminLevel);

        data.put("fightingStyle", fightingStyle);

        data.put("inventory", inventory);
        data.put("pets", pets);
        data.put("powers", powers);
        data.put("stats", stats);

        return data;
    }

    public static PlayerData fromJSON(JSONObject data) {

        String id =
            data.optString("playerId", "");

        PlayerData player =
            new PlayerData(id);

        player.nick =
            data.optString("nick", "");

        player.level =
            data.optInt("level", 1);

        player.xp =
            data.optLong("xp", 0);

        player.money =
            data.optLong("money", 0);

        player.gems =
            data.optLong("gems", 0);

        player.x =
            data.optDouble("x", 0);

        player.y =
            data.optDouble("y", 0);

        player.rank =
            data.optInt("rank", 0);

        player.adminLevel =
            data.optInt("adminLevel", 0);

        player.fightingStyle =
            data.optString(
                "fightingStyle",
                "Normal"
            );

        player.inventory =
            data.optJSONObject(
                "inventory"
            );

        if (player.inventory == null) {
            player.inventory = new JSONObject();
        }

        player.pets =
            data.optJSONObject("pets");

        if (player.pets == null) {
            player.pets = new JSONObject();
        }

        player.powers =
            data.optJSONObject("powers");

        if (player.powers == null) {
            player.powers = new JSONObject();
        }

        player.stats =
            data.optJSONObject("stats");

        if (player.stats == null) {
            player.stats = new JSONObject();
        }

        return player;
    }
          }
