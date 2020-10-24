package tshen.fb.entity;

public class FbEntityType extends FbAbstractEntityType<FbEntity> {
    public FbEntityType() {
        super(FbEntity::new);
    }
}
