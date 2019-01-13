package cat.nyaa.npc.persistence;

import cat.nyaa.nyaacore.configuration.ISerializable;

import java.util.UUID;

public class SkinData implements ISerializable, Cloneable {
    @Serializable
    public String key;
    @Serializable
    public String description;
    @Serializable
    public String texture_value;
    @Serializable
    public String texture_signature;
    @Serializable
    public UUID followPlayer; // player uuid
    @Serializable
    public int displayMask = 0x7F; // INDEX 13, https://wiki.vg/Entity_metadata#Player

    public SkinData() {
    }

    public SkinData(String key, String description, String texture_value, String texture_signature) {
        this.key = key;
        this.description = description;
        this.texture_value = texture_value;
        this.texture_signature = texture_signature;
    }

    @Override
    public SkinData clone() {
        try {
            return (SkinData) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new RuntimeException(ex);
        }
    }
}
