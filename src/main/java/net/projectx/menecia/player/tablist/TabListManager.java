package net.projectx.menecia.player.tablist;

import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import net.projectx.menecia.Menecia;
import net.projectx.menecia.resources.SkinData;
import net.projectx.menecia.npcs.NPC;
import net.projectx.menecia.npcs.NPCBuilder;
import net.projectx.menecia.utilities.Utils;
import org.bukkit.entity.Player;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class TabListManager {

    //TODO: Complete this system!

    private Map<UUID, TabList> tabListMap = new HashMap<>();
    private static boolean isClockRunning = false;

    public static void initialize(Player player, Menecia plugin) {
        player.setPlayerListHeader(
                Utils.color("&f&lMenecia\n&7(The MMORPG Minecraft Server)")
        );

        NPC header = new NPCBuilder()
                .setGameProfile(" " + 1)
                .setDisplayName("")
                .setGameMode(EnumWrappers.NativeGameMode.NOT_SET)
                .setLatency(0)
                .setSkin(SkinData.CLOCK_VALUE, SkinData.CLOCK_SIGNATURE)
                .build();
        TabListUtil.show(Collections.singletonList(header.getPlayerInfoData()), player);

        if (!isClockRunning) {
            isClockRunning = true;
            DateFormat dataFormat = new SimpleDateFormat("HH:mm:ss");
            plugin.runTaskTimer(() -> {
                TabListUtil.updateName(header.getGameProfile(), Utils.color("&7Time: &f" + dataFormat.format(new Date())));
            }, 0, 20L);
        }

        List<PlayerInfoData> playerInfoDataList = new ArrayList<>();
        NPCBuilder builder = new NPCBuilder();
        for (int i = 2; i <= 80; i++) {
            NPC NPC = builder
                    .setGameProfile(" " + i)
                    .setDisplayName("")
                    .setGameMode(EnumWrappers.NativeGameMode.NOT_SET)
                    .setLatency(0)
                    .setSkin(SkinData.DEFAULT_VALUE, SkinData.DEFAULT_SIGNATURE)
                    .build();
            playerInfoDataList.add(NPC.getPlayerInfoData());
        }
        TabListUtil.show(playerInfoDataList, player);
    }

}
