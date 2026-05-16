package client;

public class Position {
    public double x, y, z;
    public float yaw;
    public float pitch;
    public int ping;

    public int skinTextureId = -1;
    public byte[] pendingSkinPng;

    public float limbSwing;
    public float limbSwingAmount;

    public long lastAnimTime;
    public double prevAnimX, prevAnimZ;

    public Position(double x, double y, double z, float yaw, int ping) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.ping = ping;
        this.prevAnimX = x;
        this.prevAnimZ = z;
    }
}