package cat.nyaa.npc;

import cat.nyaa.npc.persistence.NpcData;
import cat.nyaa.npc.persistence.NpcTravelPlan;
import cat.nyaa.nyaacore.cmdreceiver.Arguments;
import cat.nyaa.nyaacore.cmdreceiver.BadCommandException;
import cat.nyaa.nyaacore.cmdreceiver.CommandReceiver;
import cat.nyaa.nyaacore.cmdreceiver.SubCommand;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;

public class CommandTravelPlan extends CommandReceiver {
    private final NyaaPlayerCoser plugin;

    public CommandTravelPlan(NyaaPlayerCoser plugin, I18n i18n) {
        super(plugin, i18n);
        this.plugin = plugin;
    }

    @Override
    public String getHelpPrefix() {
        return "travel";
    }

    @SubCommand(value = "disable", permission = "npc.command.edit")
    public void disableTravelPlan(CommandSender sender, Arguments args) {
        String npcId = args.nextString();
        NpcData data = plugin.cfg.npcData.npcList.get(npcId);
        if (data == null) {
            throw new BadCommandException("user.bad_id");
        }

        if (data.travelPlan == null || !data.travelPlan.isTraveller) {
            I18n.send(sender, "user.travel.not_traveler");
            return;
        }

        data.travelPlan.isTraveller = false;
        data.trades = new ArrayList<>(data.travelPlan.completeTradeIdList);
        data.x = data.travelPlan.centralX + 0.5D;
        data.y = (double)data.travelPlan.centralY;
        data.z = data.travelPlan.centralZ + 0.5D;
        plugin.entitiesManager.replaceNpcDefinition(npcId, data);
        msg(sender, "user.edit.updated");
    }

    @SubCommand(value = "enable", permission = "npc.command.edit")
    public void enableTravelPlan(CommandSender sender, Arguments args) {
        String npcId = args.nextString();
        NpcData data = plugin.cfg.npcData.npcList.get(npcId);
        if (data == null) {
            throw new BadCommandException("user.bad_id");
        }

        if (data.trades.size() <= 0)
            throw new BadCommandException("user.travel.nothing_to_trade");
        if (data.travelPlan == null) data.travelPlan = new NpcTravelPlan();
        NpcTravelPlan plan = data.travelPlan;

        if (plan.isTraveller) {
            plan.isTraveller = true;
            plan.presentTimeMinSecond = args.nextInt();
            plan.presentTimeMaxSecond = args.nextInt();
            plan.absentTimeMinSecond = args.nextInt();
            plan.absentTimeMaxSecond = args.nextInt();
            plan.availableTradeMin = Math.max(1,args.nextInt());
            plan.availableTradeMax = Math.min(args.nextInt(), plan.completeTradeIdList.size());
            plan.randomXZmax = args.nextInt();
            plan.randomYPositiveMax = args.nextInt();
            plan.randomYNegativeMax = args.nextInt();
            plan.randomTryMax = args.nextInt();
            plan.nextMovementTime = -1;
            plan.isPresent = false;
        } else {
            plan.isTraveller = true;
            plan.presentTimeMinSecond = args.nextInt();
            plan.presentTimeMaxSecond = args.nextInt();
            plan.absentTimeMinSecond = args.nextInt();
            plan.absentTimeMaxSecond = args.nextInt();
            plan.availableTradeMin = Math.max(1,args.nextInt());
            plan.availableTradeMax = Math.min(args.nextInt(), data.trades.size());
            plan.completeTradeIdList = new ArrayList<>(data.trades);
            plan.centralX = (int)Math.floor(data.x);
            plan.centralY = (int)Math.floor(data.y);
            plan.centralZ = (int)Math.floor(data.z);
            plan.randomXZmax = args.nextInt();
            plan.randomYPositiveMax = args.nextInt();
            plan.randomYNegativeMax = args.nextInt();
            plan.randomTryMax = args.nextInt();
            plan.nextMovementTime = -1;
            plan.isPresent = false;
        }

        plugin.entitiesManager.replaceNpcDefinition(npcId, data);
        msg(sender, "user.edit.updated");
    }

    @SubCommand(value = "force_move", permission = "npc.command.edit")
    public void forceTravelerMove(CommandSender sender, Arguments args) {
        String npcId = args.nextString();
        NpcData data = plugin.cfg.npcData.npcList.get(npcId);
        if (data == null) {
            throw new BadCommandException("user.bad_id");
        }

        if (data.travelPlan == null || !data.travelPlan.isTraveller) {
            I18n.send(sender, "user.travel.not_traveler");
            return;
        }

        data.travelPlan.nextMovementTime = System.currentTimeMillis() -1;
        plugin.entitiesManager.replaceNpcDefinition(npcId, data);
        msg(sender, "user.edit.updated");
    }
}
