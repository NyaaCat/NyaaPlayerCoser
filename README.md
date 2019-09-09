# NyaaPlayerCoser
NyaaPlayerCoser (NPC) plugin.

# Quick command guide

    /npc spawn VILLAGER TRADER_UNLIMITED TestNpc
    
    /npc edit help
    /npc edit 0 trade:+
    /npc edit 0 trade:+<existing_trade_id>
    /npc edit 0 trade:-<trade_id>
    /npc edit 0 trade:=<trade_id> # modify the trade specified by trade_id,
                                  # this function need a separate subcommand
                                  # You may need to reload to make this change effect on all NPCs
    /npc edit 0 trade:<trade_id>,<trade_id>,...
    
    /npc inspect nearby
    /npc inspect nearby 5
    /npc inspect npc 0
    /npc inspect trade 0
    
Custom skin if NPC's entity type is `PLAYER`. Skin info can be obtained from [https://mineskin.org/](https://mineskin.org/)

    /npc spawn PLAYER TRADER_UNLIMITED <npc_id>
    /npc skin add <skin_id> <texture_value> <texture_signature>
    /npc edit <npc_id> skin:<skin_id>
    /npc edit <npc_id> skin:default
