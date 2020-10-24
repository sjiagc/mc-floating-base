package tshen.fb;

public class FbConfig {
    public boolean isImmersive() {
        return isImmersive;
    }

    public void setImmersive(boolean immersive) {
        isImmersive = immersive;
    }

    private static FbConfig mInstance;
    private boolean isImmersive = false;

    public static FbConfig getInstance() {
        if (mInstance == null) {
            mInstance = new FbConfig();
        }
        return mInstance;
    }
}
