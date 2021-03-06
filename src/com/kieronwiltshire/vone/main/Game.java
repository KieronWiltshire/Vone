package com.kieronwiltshire.vone.main;

import com.kieronwiltshire.vone.Vone;
import com.kieronwiltshire.vone.activation.exceptions.VoneValidationException;
import com.kieronwiltshire.vone.main.events.SessionEndEvent;
import com.kieronwiltshire.vone.main.events.SessionStartEvent;
import com.kieronwiltshire.vone.main.exceptions.IncompatibleArenaSessionException;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @name Vone
 * @version 1.0
 * @author Kieron Wiltshire
 * @contact kieron.wiltshire@outlook.com
 * @copyright (c) Kieron Wiltshire 2015
 *
 * @description
 *  Vone is a 1v1, 2v2, 3v3 and Free For All (FFA) modification/plugin/extension
 *  built on top of the Bukkit and Spigot API.
 */
public class Game implements Runnable, Listener {

    // Class variables
    private static Game instance;

    /**
     * Get the Game instance
     *
     * @return Game object
     */
    public static Game getInstance() {
        if (Game.instance == null) {
            Game.instance = new Game();
        }
        return Game.instance;
    }

    // Instance variables
    private List<Profile> profiles;
    private HashMap<Arena, Session> sessions;

    /**
     * This method constructs the singleton instance
     */
    private Game() {
        this.profiles = new ArrayList<Profile>();
        this.sessions = new HashMap<Arena, Session>();
    }

    /**
     * Get the profile of a Player
     *
     * @param player Player object
     * @return Profile object
     */
    @Nullable
    public Profile getProfile(Player player) {
        for (Profile profile : this.profiles) {
            if (profile.getPlayer() == player) {
                return profile;
            }
        }
        return null;
    }

    /**
     * Add an arena to the Game object
     *
     * @param arena Arena object
     * @return true if added successfully
     */
    public boolean addArena(Arena arena) {
        if (this.sessions.containsKey(arena)) {
            return false;
        }
        this.sessions.put(arena, null);
        return true;
    }

    /**
     * Remove an arena from the Game object
     *
     * @param arena Arena object
     * @return true if removed successfully
     */
    public boolean removeArena(Arena arena) {
        if (!this.sessions.containsKey(arena)) {
            return false;
        }
        this.sessions.remove(arena);
        return true;
    }

    /**
     * Check if the Game object contains an Arena
     *
     * @param arena Arena object
     * @return true if the Game object contains the Arena
     */
    public boolean containsArena(Arena arena) {
        return this.sessions.containsKey(arena);
    }

    /**
     * Get an arena with the active session
     *
     * @param session Session object
     * @return Arena object
     */
    @Nullable
    public Arena getArena(Session session) {
        if (this.sessions.containsValue(session)) {
            for (Map.Entry<Arena, Session> k : this.sessions.entrySet()) {
                if (k.getValue() == session)
                    return k.getKey();
            }
        }
        return null;
    }

    /**
     * Set the session of an Arena
     *
     * @param arena Arena object
     * @param session Session object
     * @return true if set successfully
     */
    public boolean setSession(Arena arena, Session session) {
        if (!Vone.getInstance().isValidated()) {
            try {
                throw new VoneValidationException();
            } catch (VoneValidationException e) {
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED + e.getMessage());
            }
            return false;
        }

        if (this.sessions.get(arena) != null) {
            Bukkit.getPluginManager().callEvent(new SessionEndEvent(this.sessions.get(arena)));
            HandlerList.unregisterAll(this.sessions.get(arena));
        }

        this.sessions.remove(arena);
        this.sessions.put(arena, session);

        if (session != null) {
            try {
                session.init();
                Bukkit.getPluginManager().callEvent(new SessionStartEvent(session));
                Bukkit.getPluginManager().registerEvents(session, Vone.getInstance());
            } catch (IncompatibleArenaSessionException e) {
                if (Vone.getEnvironment().getStatus() > 0) {
                    e.printStackTrace();
                }
                this.setSession(arena, null);
                return false;
            }
        }
        return true;
    }

    /**
     * Get the Session of an Arena
     *
     * @param arena Arena object
     * @return Session object
     * @throws NullPointerException If the Arena doesn't have an active session
     */
    public Session getSession(Arena arena) throws NullPointerException {
        return this.sessions.get(arena);
    }

    /**
     * Check if a Session is active
     *
     * @param session Session object
     * @return true if the Session is active
     */
    public boolean isActive(Session session) {
        return this.sessions.containsValue(session);
    }

    /**
     * Check if an Arena is active
     *
     * @param arena Arena object
     * @return true if the Arena is active
     */
    public boolean isActive(Arena arena) {
        return this.sessions.get(arena) != null;
    }

    /**
     * Get all of the sessions currently active
     *
     * @return Session array
     */
    @Nullable
    public Session[] getSessions() {
        List<Session> sessions = new ArrayList<Session>();
        for (Session session : this.sessions.values()) {
            if (session != null) {
                sessions.add(session);
            }
        }
        if (sessions.isEmpty()) {
            return null;
        } else {
            return sessions.toArray(new Session[sessions.size()]);
        }
    }

    /**
     * Get all of the arenas that can be played
     *
     * @param active Get only the active arenas
     * @return Arena array
     */
    @Nullable
    public Arena[] getArenas(boolean active) {
        if (this.sessions.isEmpty()) {
            return null;
        } else {
            List<Arena> arenas = new ArrayList<Arena>();
            if (active) {
                for (Arena arena : this.sessions.keySet()) {
                    if (this.sessions.get(arena) != null) {
                        arenas.add(arena);
                    }
                }
                return arenas.toArray(new Arena[arenas.size()]);
            }
            return this.sessions.keySet().toArray(new Arena[this.sessions.keySet().size()]);
        }
    }

    @Override
    public void run() {
        if ((this.sessions != null) && (!this.sessions.isEmpty())) {
            for (Session session : this.sessions.values()) {
                if (session != null) {
                    session.run();
                }
            }
        }
    }

    @EventHandler
    private void onJoin(PlayerJoinEvent event) {
        if (this.getProfile(event.getPlayer()) == null) {
            this.profiles.add(new Profile(event.getPlayer()));
        }
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        if (this.getProfile(event.getPlayer()) != null) {
            this.profiles.remove(this.getProfile(event.getPlayer()));
        }
    }

}
