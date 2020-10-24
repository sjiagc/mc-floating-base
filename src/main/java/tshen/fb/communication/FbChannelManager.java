package tshen.fb.communication;

public class FbChannelManager {
    private static FbChannelManager mInstance = null;

    private FbMovementChannel mMovementChannel;

    private FbChannelManager() {
        mMovementChannel = new FbMovementChannel();
    }

    public FbMovementChannel getMovementChannel() {
        return mMovementChannel;
    }

    public static FbChannelManager getInstance() {
        if (mInstance == null) {
            mInstance = new FbChannelManager();
        }
        return mInstance;
    }
}
