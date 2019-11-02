package cam72cam.mod.entity.sync;

import cam72cam.mod.entity.Entity;
import cam72cam.mod.net.Packet;
import cam72cam.mod.util.TagCompound;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

public class EntitySync extends TagCompound {
    private final Entity entity;
    private TagCompound old;
    private String oldString;

    public EntitySync(Entity entity) {
        super();
        this.entity = entity;
        this.old = new TagCompound();
        this.oldString = old.toString();
    }

    public void send() {
        if (entity.getWorld().isClient) {
            return;
        }

        // Is this faster than the below check?
        // Could also put a bool tracker in TagCompound
        if (oldString.equals(this.toString())) {
            return;
        }

        TagCompound sync = new TagCompound();
        List<String> removed = new ArrayList<>();

        for (String key : internal.getKeys()) {
            Tag newVal = internal.getTag(key);
            if (old.internal.containsKey(key)) {
                Tag oldVal = old.internal.getTag(key);
                if (newVal.equals(oldVal)) {
                    continue;
                }
            }
            sync.internal.put(key, newVal);
        }

        for (String key : old.internal.getKeys()) {
            if (!internal.containsKey(key)) {
                removed.add(key);
            }
        }
        if (!removed.isEmpty()) {
            sync.setList("sync_internal_removed", removed, key -> {
                TagCompound tc = new TagCompound();
                tc.setString("removed", key);
                return tc;
            });
        }

        if (sync.internal.getKeys().size() != 0) {
            old = new TagCompound((CompoundTag) this.internal.copy());
            oldString = old.toString();

            entity.sendToObserving(new EntitySyncPacket(entity, sync));
        }
    }

    public void receive(TagCompound sync) {
        for (String key : sync.internal.getKeys()) {
            if (key.equals("sync_internal_removed")) {
                for (String removed : sync.getList(key, x -> x.getString("removed"))) {
                    internal.remove(removed);
                }
            } else {
                internal.put(key, sync.internal.getTag(key));
            }
        }
        old = this;
    }

    public static class EntitySyncPacket extends Packet {
        public EntitySyncPacket() {
            // Reflection
        }

        public EntitySyncPacket(Entity entity, TagCompound sync) {
            data.setEntity("target", entity);
            data.set("info", sync);
        }

        @Override
        public void handle() {
            Entity stock = data.getEntity("target", getWorld(), Entity.class);
            if (stock != null) {
                stock.sync.receive(data.get("info"));
            }
        }
    }
}
