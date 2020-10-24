package tshen.fb.entity;

import com.google.common.collect.ImmutableSet;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.EntityType;

public class FbAbstractEntityType<T extends FbEntity> extends EntityType<T> {

    public FbAbstractEntityType(EntityType.IFactory<T> inFactory) {
        super(inFactory,
              EntityClassification.MISC,
              true,
              true,
              false,
              true,
              ImmutableSet.of(),
              EntitySize.fixed(6.0f, 4.0f),
              5,
              3);
    }
}
