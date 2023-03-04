package net.mehvahdjukaar.sleep_tight.integration;

import net.mehvahdjukaar.heartstone.HeartstoneItem;
import net.minecraft.world.entity.player.Player;

public class HeartstoneCompat {

    public static boolean isFren(Player player, Player target){
        for(var v : player.getInventory().items){
            if(v.getItem() instanceof HeartstoneItem){
                if(HeartstoneItem.arePlayersBounded(player, v, target))return true;
            }
        }
        return false;
    }
}
