package rosegold.gumtuneclient.utils;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.scoreboard.*;
import rosegold.gumtuneclient.GumTuneClient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for reading scoreboard information
 * Ported from 1.8.9 Forge to 1.21.8 Fabric
 */
public class ScoreboardUtils {
    
    /**
     * Check if the scoreboard contains a specific string
     */
    public static boolean scoreboardContains(String string) {
        for (String line : getScoreboard()) {
            if (StringUtils.removeFormatting(cleanSB(line)).contains(string)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a given scoreboard list contains a specific string
     */
    public static boolean scoreboardContains(String string, List<String> scoreboard) {
        for (String line : scoreboard) {
            if (StringUtils.removeFormatting(cleanSB(line)).contains(string)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Clean scoreboard text by removing invalid characters
     */
    public static String cleanSB(String scoreboard) {
        char[] nvString = StringUtils.removeFormatting(scoreboard).toCharArray();
        StringBuilder cleaned = new StringBuilder();

        for (char c : nvString) {
            if ((int) c > 20 && (int) c < 127) {
                cleaned.append(c);
            }
        }

        return cleaned.toString();
    }

    /**
     * Get all lines from the scoreboard
     */
    public static List<String> getScoreboard() {
        List<String> lines = new ArrayList<>();
        if (GumTuneClient.mc.world == null) return lines;
        
        Scoreboard scoreboard = GumTuneClient.mc.world.getScoreboard();
        if (scoreboard == null) return lines;

        // Get the sidebar objective (slot 1)
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (objective == null) return lines;

        // Get scores
        Collection<ScoreboardEntry> scores = scoreboard.getScoreboardEntries(objective);
        List<ScoreboardEntry> list = scores.stream()
                .filter(input -> input != null && input.owner() != null && !input.owner().startsWith("#"))
                .collect(Collectors.toList());

        if (list.size() > 15) {
            scores = Lists.newArrayList(Iterables.skip(list, scores.size() - 15));
        } else {
            scores = list;
        }

        for (ScoreboardEntry entry : scores) {
            Team team = scoreboard.getScoreHolderTeam(entry.owner());
            String formatted = team == null ? entry.owner() : team.decorateName(net.minecraft.text.Text.literal(entry.owner())).getString();
            lines.add(formatted);
        }

        return lines;
    }

    /**
     * Check if currently on Hypixel by looking for "HYPIXEL" in scoreboard title
     */
    public static boolean isOnHypixel() {
        if (GumTuneClient.mc.world == null) return false;
        Scoreboard scoreboard = GumTuneClient.mc.world.getScoreboard();
        if (scoreboard == null) return false;

        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (objective == null) return false;

        String title = objective.getDisplayName().getString();
        return StringUtils.removeFormatting(title).contains("HYPIXEL");
    }
}
