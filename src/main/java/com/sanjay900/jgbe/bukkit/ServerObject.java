package com.sanjay900.jgbe.bukkit;

public class ServerObject {

    private String userName;
	private int serverId;
	private String cartName;

    public ServerObject() {
    }

    public ServerObject(int serverId, String userName, String cartName) {
        super();
        this.userName = userName;
        this.cartName = cartName;
        this. serverId = serverId;
    }

    public String getUserName() {
        return userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getcartName() {
        return cartName;
    }
    public void setMessage(String cartName) {
        this.cartName = cartName;
    }
    public int getserverIdName() {
        return serverId;
    }
    public void setserverId(int serverId) {
        this.serverId = serverId;
    }

}
