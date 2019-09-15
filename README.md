# NyaaPlayerCoser [![Build Status](https://ci.nyaacat.com/job/NyaaPlayerCoser/badge/icon)](https://ci.nyaacat.com/job/NyaaPlayerCoser/)

NyaaPlayerCoser (NPC) plugin.

# Quick command guide

    /npc spawn VILLAGER TRADER_UNLIMITED TestNpc
    
    /npc edit help
    /npc edit 0 trade:+
    /npc edit 0 trade:+<existing_trade_id>
    /npc edit 0 trade:-<trade_id>
    /npc edit_trade <trade_id>
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

# Version History
- 7.0.x: for Minecraft 1.14.4, since build 8