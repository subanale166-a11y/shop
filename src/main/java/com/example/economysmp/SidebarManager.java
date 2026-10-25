package com.example.economysmp;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import io.papermc.paper.scoreboard.numbers.NumberFormat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Sidebar scoreboard (shows on the right-middle of the screen). */
public class SidebarManager {
    private final EconomySMP plugin;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();

    public SidebarManager(EconomySMP plugin) {
        this.plugin = plugin;
    }

    public void remove(Player p) {
        boards.remove(p.getUniqueId());
        p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public void update(Player p) {
        List<String> lines = plugin.getConfig().getStringList("scoreboard.lines");
        if (lines.size() > 15) lines = lines.subList(0, 15);

        Scoreboard sb = boards.get(p.getUniqueId());
        if (sb == null) {
            sb = create(p, lines.size());
            boards.put(p.getUniqueId(), sb);
        }

        PlayerData d = plugin.data().get(p.getUniqueId());
        for (int i = 0; i < lines.size(); i++) {
            String text = lines.get(i)
                    .replace("{money}", Util.money(d.money))
                    .replace("{shards}", String.valueOf(d.shards))
                    .replace("{playtime}", Util.time(d.playtime))
                    .replace("{player}", p.getName());
            Team t = sb.getTeam("line" + i);
            if (t != null) t.prefix(Util.color(text));
        }

        if (p.getScoreboard() != sb) p.setScoreboard(sb);
    }

    private Scoreboard create(Player p, int lineCount) {
        Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = sb.registerNewObjective("sidebar", Criteria.DUMMY,
                Util.color(plugin.getConfig().getString("scoreboard.title", "&b&lSMP")));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.numberFormat(NumberFormat.blank()); // hide the red score numbers

        for (int i = 0; i < lineCount; i++) {
            String entry = "\u00a7" + Integer.toHexString(i); // unique invisible entry
            Team t = sb.registerNewTeam("line" + i);
            t.addEntry(entry);
            obj.getScore(entry).setScore(lineCount - i);
        }
        return sb;
    }
}
