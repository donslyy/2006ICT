package app;

import java.util.*;
import java.util.function.Consumer;

public final class ConfigService {
    private static final ConfigService INSTANCE = new ConfigService();
    public static ConfigService getInstance() { return INSTANCE; }
    private ConfigService() { applyDefaults(); }

    public enum PlayerType { HUMAN, AI, EXTERNAL }
    public enum Mode { ONE_PLAYER, TWO_PLAYER }

    private int fieldWidth, fieldHeight, blockSize, startLevel;
    private boolean musicEnabled, sfxEnabled, aiPlay, extendedMode;
    private String serverHost = "localhost";
    private int serverPort = 3000;
    private Mode mode = Mode.ONE_PLAYER;
    private PlayerType player1Type = PlayerType.HUMAN, player2Type = PlayerType.AI;

    private final List<Consumer<ConfigService>> listeners = new ArrayList<>();
    public void addListener(Consumer<ConfigService> l){ if(l!=null) listeners.add(l); }
    public void removeListener(Consumer<ConfigService> l){ listeners.remove(l); }
    private void fire(){ for (var l : List.copyOf(listeners)) l.accept(this); }

    public void applyDefaults() {
        fieldWidth  = 10;   // was 15
        fieldHeight = 20;   // stays 20
        startLevel  = 1;
        musicEnabled = false;
        sfxEnabled   = false;
        aiPlay       = false;
        extendedMode = false;
        blockSize    = 24;
        serverHost   = "localhost";
        serverPort   = 3000;
        player1Type  = PlayerType.HUMAN;
        player2Type  = PlayerType.AI;
        fire();
    }

    // getters
    public int getFieldWidth(){return fieldWidth;}  public int getFieldHeight(){return fieldHeight;}
    public int getBlockSize(){return blockSize;}    public int getStartLevel(){return startLevel;}
    public boolean isMusicEnabled(){return musicEnabled;} public boolean isSfxEnabled(){return sfxEnabled;}
    public boolean isAiPlay(){return aiPlay;}       public boolean isExtendedMode(){return extendedMode;}
    public String getServerHost(){return serverHost;} public int getServerPort(){return serverPort;}
    public Mode getMode(){return mode;} public PlayerType getPlayer1Type(){return player1Type;} public PlayerType getPlayer2Type(){return player2Type;}

    // setters (notify on change)
    public void setFieldWidth(int v){ if(v!=fieldWidth){ fieldWidth=v; fire(); } }
    public void setFieldHeight(int v){ if(v!=fieldHeight){ fieldHeight=v; fire(); } }
    public void setBlockSize(int v){ if(v!=blockSize){ blockSize=v; fire(); } }
    public void setStartLevel(int v){ if(v!=startLevel){ startLevel=v; fire(); } }
    public void setMusicEnabled(boolean v){ if(v!=musicEnabled){ musicEnabled=v; fire(); } }
    public void setSfxEnabled(boolean v){ if(v!=sfxEnabled){ sfxEnabled=v; fire(); } }
    public void setAiPlay(boolean v){ if(v!=aiPlay){ aiPlay=v; fire(); } }
    public void setExtendedMode(boolean v){ if(v!=extendedMode){ extendedMode=v; fire(); } }
    public void setServerHost(String v){ if(!Objects.equals(v,serverHost)){ serverHost=v; fire(); } }
    public void setServerPort(int v){ if(v!=serverPort){ serverPort=v; fire(); } }
    public void setMode(Mode v){ if(v!=mode){ mode=v; fire(); } }
    public void setPlayer1Type(PlayerType v){ if(v!=player1Type){ player1Type=v; fire(); } }
    public void setPlayer2Type(PlayerType v){ if(v!=player2Type){ player2Type=v; fire(); } }
}
