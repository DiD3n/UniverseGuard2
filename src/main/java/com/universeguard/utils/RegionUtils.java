/* 
 * Copyright (C) JimiIT92 - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Jimi, December 2017
 * 
 */
package com.universeguard.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.universeguard.region.GlobalRegion;
import com.universeguard.region.LocalRegion;
import com.universeguard.region.Region;
import com.universeguard.region.components.RegionCommand;
import com.universeguard.region.components.RegionExplosion;
import com.universeguard.region.components.RegionFlag;
import com.universeguard.region.components.RegionInteract;
import com.universeguard.region.components.RegionLocation;
import com.universeguard.region.components.RegionMember;
import com.universeguard.region.components.RegionMob;
import com.universeguard.region.components.RegionVehicle;
import com.universeguard.region.enums.EnumRegionExplosion;
import com.universeguard.region.enums.EnumRegionFlag;
import com.universeguard.region.enums.EnumRegionInteract;
import com.universeguard.region.enums.EnumRegionVehicle;
import com.universeguard.region.enums.RegionEventType;
import com.universeguard.region.enums.RegionPermission;
import com.universeguard.region.enums.RegionRole;
import com.universeguard.region.enums.RegionText;
import com.universeguard.region.enums.RegionType;

/**
 * 
 * Utility class for regions
 * @author Jimi
 *
 */
public class RegionUtils {

	// Pending Regions
	private static HashMap<Player, Region> PENDINGS = new HashMap<Player, Region>();

	/**
	 * Save a Region to a JSON file
	 * @param region The Region
	 * @return true if the Region has been saved correctly, false otherwise
	 */
	public static boolean save(Region region) {
		Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
		FileWriter fileWriter = null;
		try {
			File directory = region.isLocal() ? getRegionFolder() : getGlobalRegionFolder();
			if (!directory.exists())
				directory.mkdirs();
			File file = getFile(region);
			if (!file.exists())
				file.createNewFile();
			fileWriter = new FileWriter(file);
			fileWriter.write(gson.toJson(region));
			return true;
		} catch (IOException e) {
			LogUtils.log(e);
			LogUtils.print(TextColors.RED, RegionText.REGION_SAVE_EXCEPTION.getValue());
			return false;
		} finally {
			if (fileWriter != null) {
				try {
					fileWriter.close();
				} catch (IOException e) {
					LogUtils.log(e);
					LogUtils.print(RegionText.REGION_WRITER_CLOSE_EXCEPTION.getValue());
				}
			}
		}
	}

	/**
	 * Remove a Region from the regions folder
	 * @param region The Region
	 * @return true if the Region has been removed correctly, false otherwise
	 */
	public static boolean remove(Region region) {
		File directory = region.isLocal() ? getRegionFolder() : getGlobalRegionFolder();
		if (!directory.exists())
			return false;
		File file = getFile(region);
		if (file.exists())
			return file.delete();

		return false;
	}

	/**
	 * Get a Region from name
	 * @param name The name of the region
	 * @return The Region with that name if exists, null otherwise
	 */
	public static Region load(String name) {
		for (Region region : getAllRegions()) {
			if (region.getName().equalsIgnoreCase(name))
				return region;
		}
		return null;
	}

	/**
	 * Update a Region to the latest RegionVersion
	 * @param region The Region to update
	 */
	public static void update(Region region) {
		if (region.isLocal()) {
			LocalRegion newRegion = new LocalRegion(region.getName(), ((LocalRegion) region).getFirstPoint(),
					((LocalRegion) region).getSecondPoint());
			newRegion.setMembers(((LocalRegion) region).getMembers());
			newRegion.setPriority(((LocalRegion) region).getPriority());
			newRegion.setSpawnLocation(((LocalRegion) region).getSpawnLocation());
			newRegion.setTeleportLocation(((LocalRegion) region).getTeleportLocation());
			newRegion.setFlags(region.getFlags());
			newRegion.setCommands(region.getCommands());
			newRegion.setInteracts(region.getInteracts());
			newRegion.setVehicles(region.getVehicles());
			newRegion.setExplosions(region.getExplosions());
			newRegion.setMobs(region.getMobs());
			newRegion.setGamemode(region.getGameMode());
			newRegion.setName(region.getName());
			newRegion.updateFlags();
			save(newRegion);
		} else {
			GlobalRegion newRegion = new GlobalRegion(region.getName());
			newRegion.setFlags(region.getFlags());
			newRegion.setGamemode(region.getGameMode());
			newRegion.setCommands(region.getCommands());
			newRegion.setInteracts(region.getInteracts());
			newRegion.setVehicles(region.getVehicles());
			newRegion.setExplosions(region.getExplosions());
			newRegion.setMobs(region.getMobs());
			newRegion.updateFlags();
			save(newRegion);
		}
	}

	/**
	 * Get All Regions
	 * @return The list of all Regions
	 */
	public static ArrayList<Region> getAllRegions() {
		ArrayList<Region> regions = new ArrayList<Region>();
		if (getRegionFolder().exists())
			regions.addAll(loadRegions(getRegionFolder(), RegionType.LOCAL));
		if (getGlobalRegionFolder().exists())
			regions.addAll(loadRegions(getGlobalRegionFolder(), RegionType.GLOBAL));
		return regions;
	}

	/**
	 * Check if there are old regions to convert
	 */
	public static boolean shouldConvertOldRegions()
	{
		return getOldRegionFolder().exists() || getOldGlobalRegionFolder().exists();
	}
	
	/**
	 * Convert the old region format to the new one (from UniverseGuard to UniverseGuard2)
	 */
	public static void convertOldRegions()
	{
		if(getOldRegionFolder().exists())
			loadOldRegions(getOldRegionFolder(), RegionType.LOCAL);
		if(getOldGlobalRegionFolder().exists())
			loadOldRegions(getOldGlobalRegionFolder(), RegionType.GLOBAL);
	}
	
	/**
	 * Convert the old region format to the new one (from UniverseGuard to UniverseGuard2)
	 * @param directory The folder where the old regions are
	 * @param type The RegionType to convert
	 */
	public static void loadOldRegions(File directory, RegionType type) {		
		for (File file : directory.listFiles()) {
			BufferedReader bufferedReader = null;
			try {
				bufferedReader = new BufferedReader(new FileReader(file));
				JsonObject jsonObject = new JsonObject();
				JsonParser parser = new JsonParser();
	            JsonElement jsonElement = parser.parse(new FileReader(file));
	            jsonObject = jsonElement.getAsJsonObject();
	            for (Iterator<Entry<String, JsonElement>> elements = jsonObject.entrySet().iterator(); elements.hasNext();){
	                Entry<String, JsonElement> element = elements.next();
	            	JsonObject jObj = element.getValue().getAsJsonObject();
	            	Region region = null;
	            	if(type.equals(RegionType.LOCAL))
	            		region = new LocalRegion(element.getKey());
	            	else
	            		region = new GlobalRegion(element.getKey());
	            	
	            	JsonObject flagObject = jObj.getAsJsonObject("flags");
	            	for(EnumRegionFlag flag : EnumRegionFlag.values()) {
	            		JsonElement flagElement = flagObject.get(flag.getName().toLowerCase());
	            		region.setFlag(flag, flagElement != null ? flagElement.getAsBoolean() : flag.getValue());
	            	}
	            	JsonElement flagBuildElement = flagObject.get("build");
	            	region.setFlag(EnumRegionFlag.PLACE, flagBuildElement != null ? flagBuildElement.getAsBoolean() : EnumRegionFlag.PLACE.getValue());
	            	region.setFlag(EnumRegionFlag.DESTROY, flagBuildElement != null ? flagBuildElement.getAsBoolean() : EnumRegionFlag.DESTROY.getValue());
	            	
	            	JsonElement flagChestsElement = flagObject.get("chests");
	            	region.setFlag(EnumRegionFlag.CHESTS, flagChestsElement != null ? flagChestsElement.getAsBoolean() : EnumRegionFlag.CHESTS.getValue());
	            	region.setFlag(EnumRegionFlag.TRAPPED_CHESTS, flagChestsElement != null ? flagChestsElement.getAsBoolean() : EnumRegionFlag.TRAPPED_CHESTS.getValue());
	            	
	            	JsonElement flagUseElement = flagObject.get("use");
	            	for(EnumRegionInteract interact : EnumRegionInteract.values())
	            		region.setInteract(interact, flagUseElement != null ? flagUseElement.getAsBoolean() : interact.getValue());
	            	
	            	JsonElement flagVehiclePlaceElement = flagObject.get("vehicleplace");
	            	for(EnumRegionVehicle vehicle : EnumRegionVehicle.values())
	            		region.setVehiclePlace(vehicle, flagVehiclePlaceElement != null ? flagVehiclePlaceElement.getAsBoolean() : vehicle.getPlace());
	            	
	            	JsonElement flagVehicleDestroyElement = flagObject.get("vehicledestroy");
	            	for(EnumRegionVehicle vehicle : EnumRegionVehicle.values())
	            		region.setVehicleDestroy(vehicle, flagVehicleDestroyElement != null ? flagVehicleDestroyElement.getAsBoolean() : vehicle.getDestroy());
	            	
	            	JsonElement flagExplosionDestroyElement = flagObject.get("otherexplosions");
	            	for(EnumRegionExplosion explosion : EnumRegionExplosion.values())
	            		region.setExplosionDestroy(explosion, flagExplosionDestroyElement != null ? flagExplosionDestroyElement.getAsBoolean() : explosion.getDestroy());
	            	
	            	JsonElement flagExplosionDamageElement = flagObject.get("otherexplosionsdamage");
	            	for(EnumRegionExplosion explosion : EnumRegionExplosion.values())
	            		region.setExplosionDamage(explosion, flagExplosionDamageElement != null ? flagExplosionDamageElement.getAsBoolean() : explosion.getDamage());
	            	
	            	JsonElement flagTntElement = flagObject.get("tnt");
	            	region.setExplosionDestroy(EnumRegionExplosion.TNT, flagTntElement != null ? flagTntElement.getAsBoolean() : EnumRegionExplosion.TNT.getDestroy());
	            	JsonElement flagTntDamageElement = flagObject.get("tntdamage");
	            	region.setExplosionDamage(EnumRegionExplosion.TNT, flagTntDamageElement != null ? flagTntDamageElement.getAsBoolean() : EnumRegionExplosion.TNT.getDamage());
	            	
	            	JsonElement flagCreeperElement = flagObject.get("creeperexplosions");
	            	region.setExplosionDestroy(EnumRegionExplosion.CREEPER, flagCreeperElement != null ? flagCreeperElement.getAsBoolean() : EnumRegionExplosion.CREEPER.getDestroy());
	            	JsonElement flagCreeperDamageElement = flagObject.get("mobdamage");
	            	region.setExplosionDamage(EnumRegionExplosion.CREEPER, flagCreeperDamageElement != null ? flagCreeperDamageElement.getAsBoolean() : EnumRegionExplosion.CREEPER.getDamage());
	            	
	            	if(type.equals(RegionType.LOCAL)) {
	            		JsonObject firstPoint = jObj.getAsJsonObject("pos1");
	            		JsonObject secondPoint = jObj.getAsJsonObject("pos2");
	            		JsonObject teleportPoint = jObj.getAsJsonObject("teleport");
	            		JsonObject spawnPoint = jObj.getAsJsonObject("spawn");
	            		((LocalRegion)region).setFirstPoint(new RegionLocation(firstPoint.get("x").getAsInt(), firstPoint.get("y").getAsInt(), firstPoint.get("z").getAsInt(), jObj.get("dimension").getAsString(), jObj.get("world").getAsString()));
	            		((LocalRegion)region).setSecondPoint(new RegionLocation(secondPoint.get("x").getAsInt(), secondPoint.get("y").getAsInt(), secondPoint.get("z").getAsInt(), jObj.get("dimension").getAsString(), jObj.get("world").getAsString()));
	            		((LocalRegion)region).setTeleportLocation(new RegionLocation(teleportPoint.get("x").getAsInt(), teleportPoint.get("y").getAsInt(), teleportPoint.get("z").getAsInt(), jObj.get("dimension").getAsString(), jObj.get("world").getAsString()));
	            		((LocalRegion)region).setSpawnLocation(new RegionLocation(spawnPoint.get("x").getAsInt(), spawnPoint.get("y").getAsInt(), spawnPoint.get("z").getAsInt(), jObj.get("dimension").getAsString(), jObj.get("world").getAsString()));
	            		((LocalRegion)region).setPriority(jObj.get("priority").getAsInt());
	            		((LocalRegion)region).setGamemode(jObj.get("gamemode").getAsString());
	            	}
	            	
	            	save(region);
	              }

			} catch (FileNotFoundException e) {
				LogUtils.log(e);
				LogUtils.print(TextColors.RED, RegionText.REGION_LOAD_EXCEPTION.getValue());
			} finally {
				if (bufferedReader != null) {
					try {
						bufferedReader.close();
					} catch (IOException e) {
						LogUtils.log(e);
						LogUtils.print(TextColors.RED, RegionText.REGION_READER_CLOSE_EXCEPTION.getValue());
					}
				}
			}
		}
	}
	
	/**
	 * Get all Regions of the given type
	 * @param directory The folder where the regions are
	 * @param type The RegionType to load
	 * @return The list of the Regions of that given RegionType
	 */
	public static ArrayList<Region> loadRegions(File directory, RegionType type) {
		ArrayList<Region> regions = new ArrayList<Region>();
		for (File file : directory.listFiles()) {
			Gson gson = new Gson();
			BufferedReader bufferedReader = null;
			try {
				bufferedReader = new BufferedReader(new FileReader(file));
				if (type == RegionType.LOCAL) {
					LocalRegion region = gson.fromJson(bufferedReader, LocalRegion.class);
					if (region != null)
						regions.add(region);
				} else {
					GlobalRegion region = gson.fromJson(bufferedReader, GlobalRegion.class);
					if (region != null)
						regions.add(region);
				}
			} catch (FileNotFoundException e) {
				LogUtils.log(e);
				LogUtils.print(TextColors.RED, RegionText.REGION_LOAD_EXCEPTION.getValue());
			} finally {
				if (bufferedReader != null) {
					try {
						bufferedReader.close();
					} catch (IOException e) {
						LogUtils.log(e);
						LogUtils.print(TextColors.RED, RegionText.REGION_READER_CLOSE_EXCEPTION.getValue());
					}
				}
			}
		}
		return regions;
	}

	/**
	 * Set the pending Region for a player
	 * @param player The player
	 * @param region The Region
	 */
	public static void setPendingRegion(Player player, Region region) {
		if (region == null)
			PENDINGS.remove(player);
		else if (!PENDINGS.containsKey(player)) {
			PENDINGS.put(player, region);
		}
	}
	
	/**
	 * Get the pending region for a player
	 * @param player The player
	 * @return The pending Region of the player if exists, null otherwise
	 */
	public static Region getPendingRegion(Player player) {
		return PENDINGS.get(player);
	}

	/**
	 * Update the pending region of a player
	 * @param player The player
	 * @param region The pending Region
	 */
	public static void updatePendingRegion(Player player, Region region) {
		setPendingRegion(player, null);
		setPendingRegion(player, region);
	}

	/**
	 * Check if a player has a pending Region
	 * @param player The player
	 * @return true if the player has a pending Region, false otherwise
	 */
	public static boolean hasPendingRegion(Player player) {
		return getPendingRegion(player) != null;
	}

	/**
	 * Shows the list of all Regions to a player
	 * @param player The player
	 */
	public static void printRegionsList(Player player) {
		StringBuilder regions = new StringBuilder();
		for (Region region : getAllRegions()) {
			regions.append(region.getName() + ", ");
		}
		MessageUtils.sendMessage(player, RegionText.REGION_LIST.getValue(), TextColors.GOLD);
		MessageUtils.sendMessage(player, regions.substring(0, regions.length() - 2), TextColors.YELLOW);
	}

	/**
	 * Check if a player is online
	 * @param uuid The UUID of the player
	 * @return true if the player with that UUID is online, false otherwise
	 */
	public static boolean isOnline(UUID uuid) {
		return Sponge.getServer().getPlayer(uuid).isPresent();
	}

	/**
	 * Shows the region informations to a player
	 * @param player The player
	 * @param region The Region
	 */
	public static void printRegion(Player player, Region region) {
		MessageUtils.sendMessage(player, RegionText.REGION_INFO.getValue() + ": " + region.getName(), TextColors.GOLD);
		MessageUtils.sendMessage(player, RegionText.TYPE.getValue() + ": " + region.getType().toString(),
				TextColors.YELLOW);
		if (region.isLocal()) {
			LocalRegion localRegion = (LocalRegion) region;
			MessageUtils.sendMessage(player,
					RegionText.PRIORITY.getValue() + ": " + String.valueOf(localRegion.getPriority()),
					TextColors.YELLOW);
			if (!localRegion.getFlag(EnumRegionFlag.HIDE_LOCATIONS)) {
				MessageUtils.sendMessage(player,
						RegionText.FROM.getValue() + ": " + localRegion.getFirstPoint().toString(), TextColors.AQUA);
				MessageUtils.sendMessage(player,
						RegionText.TO.getValue() + ": " + localRegion.getSecondPoint().toString(), TextColors.AQUA);
				MessageUtils.sendMessage(player,
						RegionText.TELEPORT.getValue() + ": " + localRegion.getTeleportLocation().toString(),
						TextColors.AQUA);
				MessageUtils.sendMessage(player,
						RegionText.SPAWN.getValue() + ": " + localRegion.getSpawnLocation().toString(),
						TextColors.AQUA);
			}
			if (!localRegion.getFlag(EnumRegionFlag.HIDE_MEMBERS)) {
				MessageUtils.sendMessage(player, RegionText.MEMBERS.getValue(), TextColors.YELLOW);
				ArrayList<Text> members = new ArrayList<Text>();
				for (int i = 0; i < localRegion.getMembers().size(); i++) {
					RegionMember member = localRegion.getMembers().get(i);
					members.add(Text.of(
							member.getUUID().equals(player.getUniqueId()) ? TextColors.AQUA
									: isOnline(member.getUUID()) ? TextColors.GREEN : TextColors.RED,
							member.getUsername(), i < localRegion.getMembers().size() - 1 ? ", " : ""));
				}
				player.sendMessage(Text.of(members.toArray()));
			}
		}
		if (!region.getFlag(EnumRegionFlag.HIDE_FLAGS)) {
			MessageUtils.sendMessage(player, RegionText.FLAGS.getValue(), TextColors.YELLOW);
			ArrayList<Text> flags = new ArrayList<Text>();
			for (int i = 0; i < region.getFlags().size(); i++) {
				RegionFlag flag = region.getFlags().get(i);
				flags.add(Text.of(flag.getValue() ? TextColors.GREEN : TextColors.RED, flag.getName(),
						i < region.getFlags().size() - 1 ? ", " : ""));
			}
			player.sendMessage(Text.of(flags.toArray()));
			MessageUtils.sendMessage(player, RegionText.INTERACTS.getValue(), TextColors.YELLOW);
			ArrayList<Text> interacts = new ArrayList<Text>();
			for (int i = 0; i < region.getInteracts().size(); i++) {
				RegionInteract interact = region.getInteracts().get(i);
				interacts.add(Text.of(interact.isEnabled() ? TextColors.GREEN : TextColors.RED, interact.getBlock(),
						i < region.getInteracts().size() - 1 ? ", " : ""));
			}
			player.sendMessage(Text.of(interacts.toArray()));
			MessageUtils.sendMessage(player, RegionText.EXPLOSIONS_DAMAGE.getValue(), TextColors.YELLOW);
			ArrayList<Text> explosionsDamage = new ArrayList<Text>();
			for (int i = 0; i < region.getExplosions().size(); i++) {
				RegionExplosion explosion = region.getExplosions().get(i);
				explosionsDamage.add(Text.of(explosion.getDamage() ? TextColors.GREEN : TextColors.RED,
						explosion.getExplosion(), i < region.getExplosions().size() - 1 ? ", " : ""));
			}
			player.sendMessage(Text.of(explosionsDamage.toArray()));
			MessageUtils.sendMessage(player, RegionText.EXPLOSIONS_DESTROY.getValue(), TextColors.YELLOW);
			ArrayList<Text> explosionsDestroy = new ArrayList<Text>();
			for (int i = 0; i < region.getExplosions().size(); i++) {
				RegionExplosion explosion = region.getExplosions().get(i);
				explosionsDestroy.add(Text.of(explosion.getDestroy() ? TextColors.GREEN : TextColors.RED,
						explosion.getExplosion(), i < region.getExplosions().size() - 1 ? ", " : ""));
			}
			player.sendMessage(Text.of(explosionsDestroy.toArray()));

			MessageUtils.sendMessage(player, RegionText.VEHICLES_PLACE.getValue(), TextColors.YELLOW);
			ArrayList<Text> vehiclesPlace = new ArrayList<Text>();
			for (int i = 0; i < region.getVehicles().size(); i++) {
				RegionVehicle vehicle = region.getVehicles().get(i);
				vehiclesPlace.add(Text.of(vehicle.getPlace() ? TextColors.GREEN : TextColors.RED, vehicle.getName(),
						i < region.getVehicles().size() - 1 ? ", " : ""));
			}
			player.sendMessage(Text.of(vehiclesPlace.toArray()));

			MessageUtils.sendMessage(player, RegionText.VEHICLES_DESTROY.getValue(), TextColors.YELLOW);
			ArrayList<Text> vehiclesDestroy = new ArrayList<Text>();
			for (int i = 0; i < region.getVehicles().size(); i++) {
				RegionVehicle vehicle = region.getVehicles().get(i);
				vehiclesDestroy.add(Text.of(vehicle.getDestroy() ? TextColors.GREEN : TextColors.RED, vehicle.getName(),
						i < region.getVehicles().size() - 1 ? ", " : ""));
			}
			player.sendMessage(Text.of(vehiclesDestroy.toArray()));

			if (region.getMobs().size() > 0) {
				MessageUtils.sendMessage(player, RegionText.MOBS_SPAWN.getValue(), TextColors.YELLOW);
				ArrayList<Text> mobsSpawn = new ArrayList<Text>();
				for (int i = 0; i < region.getMobs().size(); i++) {
					RegionMob mob = region.getMobs().get(i);
					mobsSpawn.add(Text.of(mob.getSpawn() ? TextColors.GREEN : TextColors.RED, mob.getMob(),
							i < region.getMobs().size() - 1 ? ", " : ""));
				}
				player.sendMessage(Text.of(mobsSpawn.toArray()));

				MessageUtils.sendMessage(player, RegionText.MOBS_PVE.getValue(), TextColors.YELLOW);
				ArrayList<Text> mobsPve = new ArrayList<Text>();
				for (int i = 0; i < region.getMobs().size(); i++) {
					RegionMob mob = region.getMobs().get(i);
					mobsPve.add(Text.of(mob.getPve() ? TextColors.GREEN : TextColors.RED, mob.getMob(),
							i < region.getMobs().size() - 1 ? ", " : ""));
				}
				player.sendMessage(Text.of(mobsPve.toArray()));

				MessageUtils.sendMessage(player, RegionText.MOBS_DAMAGE.getValue(), TextColors.YELLOW);
				ArrayList<Text> mobsDamage = new ArrayList<Text>();
				for (int i = 0; i < region.getMobs().size(); i++) {
					RegionMob mob = region.getMobs().get(i);
					mobsDamage.add(Text.of(mob.getDamage() ? TextColors.GREEN : TextColors.RED, mob.getMob(),
							i < region.getMobs().size() - 1 ? ", " : ""));
				}
				player.sendMessage(Text.of(mobsDamage.toArray()));
				
				MessageUtils.sendMessage(player, RegionText.MOBS_DROP.getValue(), TextColors.YELLOW);
				ArrayList<Text> mobsDrop = new ArrayList<Text>();
				for (int i = 0; i < region.getMobs().size(); i++) {
					RegionMob mob = region.getMobs().get(i);
					mobsDrop.add(Text.of(mob.getDrop() ? TextColors.GREEN : TextColors.RED, mob.getMob(),
							i < region.getMobs().size() - 1 ? ", " : ""));
				}
				player.sendMessage(Text.of(mobsDrop.toArray()));
				
				MessageUtils.sendMessage(player, RegionText.COMMANDS.getValue(), TextColors.YELLOW);
				ArrayList<Text> commands = new ArrayList<Text>();
				for (int i = 0; i < region.getCommands().size(); i++) {
					RegionCommand command = region.getCommands().get(i);
					commands.add(Text.of(command.isEnabled() ? TextColors.GREEN : TextColors.RED, command.getCommand(),
							i < region.getCommands().size() - 1 ? ", " : ""));
				}
				player.sendMessage(Text.of(commands.toArray()));
			}

		}
	}
	
	/**
	 * Get a player from it's UUID
	 */
	public static Player getPlayer(UUID uuid) {
	    Optional<Player> onlinePlayer = Sponge.getServer().getPlayer(uuid);

	    if (onlinePlayer.isPresent()) {
	        return onlinePlayer.get();
	    }

	    Optional<UserStorageService> userStorage = Sponge.getServiceManager().provide(UserStorageService.class);

	    return userStorage.isPresent() ? userStorage.get().get(uuid).get().getPlayer().get() : null;
	}
	
	/**
	 * Get the member of a Region
	 * @param region The Region
	 * @param player The player
	 * @return The RegionMember if the player is a member of that Region, null otherwise
	 */
	public static RegionMember getMember(LocalRegion region, Player player) {
		for (RegionMember member : region.getMembers()) {
			if (member.getUUID().equals(player.getUniqueId()))
				return member;
		}
		return null;
	}

	/**
	 * Check if a player is a member of a Region
	 * @param region The Region
	 * @param player The player
	 * @return true if the player is a member of that Region, false otherwise
	 */
	public static boolean isMemberByUUID(Region region, UUID player) {
		return isMember(region, getPlayer(player));
	}
	
	/**
	 * Check if a player is a member of a Region
	 * @param region The Region
	 * @param player The player
	 * @return true if the player is a member of that Region, false otherwise
	 */
	public static boolean isMember(Region region, Player player) {
		if (region.isLocal()) {
			return getMember((LocalRegion) region, player) != null;
		}
		return false;
	}

	/**
	 * Check if a player is the owner of a Region
	 * @param region The Region
	 * @param player The player
	 * @return true is the player is the owner of that Region, false otherwise
	 */
	public static boolean isOwner(Region region, Player player) {
		if (region.isLocal()) {
			return isMember(region, player)
					&& getMember((LocalRegion) region, player).getRole().equals(RegionRole.OWNER);
		}
		return false;
	}

	/**
	 * Check if a player has a Region permission
	 * @param player The Player
	 * @param region The Region
	 * @return true if the player has permissions in that Region, false otherwise
	 */
	public static boolean hasPermission(Player player, Region region) {
		return PermissionUtils.hasPermission(player, RegionPermission.REGION) || isMember(region, player);
	}
	
	/**
	 * Check if a player has a Region
	 * @param player The player
	 * @return true if the player has a Region, false otherwise
	 */
	public static boolean hasRegion(Player player) {
		for (Region region : getAllRegions()) {
			if (isMember(region, player))
				return true;
		}
		return false;
	}

	/**
	 * Shows the Help page header for a player 
	 * @param player The player
	 * @param page The page to display
	 */
	public static void printHelpHeader(Player player, int page) {
		MessageUtils.sendMessage(player, RegionText.HELP.getValue() + "(" + String.valueOf(page) + "/4)",
				TextColors.GOLD);
	}

	/**
	 * Shows the FlagHelp page header for a player 
	 * @param player The player
	 * @param page The page to display
	 */
	public static void printFlagHelpHeader(Player player, int page) {
		MessageUtils.sendMessage(player, RegionText.FLAG_HELP.getValue() + "(" + String.valueOf(page) + "/10)",
				TextColors.GOLD);
	}

	/**
	 * Shows the command help text for a player 
	 * @param player The player
	 * @param command The command
	 * @param text The help text
	 */
	public static void printHelpFor(Player player, String command, RegionText text) {
		MessageUtils.sendMessage(player, "/rg " + command + " - " + text.getValue(), TextColors.YELLOW);
	}

	/**
	 * Shows the flag help text for a player 
	 * @param player The player
	 * @param flag The flag
	 * @param text The help text
	 */
	public static void printFlagHelpFor(Player player, EnumRegionFlag flag, RegionText text) {
		printFlagHelpFor(player, flag.getName(), text);
	}

	/**
	 * Shows the flag help text for a player 
	 * @param player The player
	 * @param flag The flag
	 * @param text The help text
	 */
	public static void printFlagHelpFor(Player player, String flag, RegionText text) {
		MessageUtils.sendMessage(player, flag + " - " + text.getValue(), TextColors.YELLOW);
	}


	/**
	 * Check if a location is in a Region
	 * @param region The Region
	 * @param location The location
	 * @return true if that location is in that Region, false otherwise
	 */
	public static boolean isInRegion(LocalRegion region, Location<World> location) {
		Location<World> pos1 = region.getFirstPoint().getLocation();
		Location<World> pos2 = region.getSecondPoint().getLocation();
		if(pos1 != null && pos2 != null) {
			int x1 = Math.min(pos1.getBlockX(), pos2.getBlockX());
			int y1 = Math.min(pos1.getBlockY(), pos2.getBlockY());
			int z1 = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
			int x2 = Math.max(pos1.getBlockX(), pos2.getBlockX());
			int y2 = Math.max(pos1.getBlockY(), pos2.getBlockY());
			int z2 = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

			return region.getWorld().equals(region.getWorld())
					&& region.getFirstPoint().getDimension().equalsIgnoreCase(location.getExtent().getDimension().getType().getId())
					&& ((location.getBlockX() >= x1 && location.getBlockX() <= x2) && (location.getBlockY() >= y1 && location.getBlockY() <= y2)
							&& (location.getBlockZ() >= z1 && location.getBlockZ() <= z2));
		}
		return false;
	}

	/**
	 * Get a Region at a location
	 * @param location The location
	 * @return The Region at the given location if exists, the GlobalRegion of the location world otherwise
	 */
	public static Region getRegion(Location<World> location) {
		LocalRegion localRegion = getLocalRegion(location);
		return localRegion != null ? localRegion : getGlobalRegion(location);
	}
	
	/**
	 * Get a LocalRegion at a location
	 * @param location The location
	 * @return The LocalRegion at the given location if exists, null otherwise
	 */
	public static LocalRegion getLocalRegion(Location<World> location) {
		LocalRegion region = null;
		for(Region r : getAllRegions()) {
			if(r.isLocal() && isInRegion((LocalRegion)r, location)) {
				if(region == null || ((LocalRegion)r).getPriority() >= region.getPriority())
					region = ((LocalRegion)r);
			}
		}
		return region;
	}
	
	/**
	 * Get a GlobalRegion at a location
	 * @param location The location
	 * @return The GlobalRegion at the given location
	 */
	public static GlobalRegion getGlobalRegion(Location<World> location) {
		return (GlobalRegion)load(location.getExtent().getName());
	}

	/**
	 * Handle a Flag event
	 * @param event The event
	 * @param flag The flag
	 * @param location The location
	 * @param player The player
	 * @param type The EventType
	 * @return true if the event has been cancelled, false otherwise
	 */
	public static boolean handleEvent(Cancellable event, EnumRegionFlag flag, Location<World> location, Player player, RegionEventType type) {
		Region region = RegionUtils.getRegion(location);
		return handleEvent(event, flag, region, player, type);
	}
	
	/**
	 * Handle a Flag event for a Region
	 * @param event The event
	 * @param flag The flag
	 * @param region The Region
	 * @param location The location
	 * @param player The player
	 * @param type The EventType
	 * @return true if the event has been cancelled, false otherwise
	 */
	private static boolean handleEvent(Cancellable event, EnumRegionFlag flag, Region region, Player player, RegionEventType type) {
		if(region != null) {
			boolean cancel = flag.equals(EnumRegionFlag.INVINCIBLE) ? region.getFlag(flag) : !region.getFlag(flag);
			if(player != null) {
				if(type.equals(RegionEventType.LOCAL) && region.isLocal())
					cancel = !RegionUtils.hasPermission(player, region);
				else
					cancel = cancel && !PermissionUtils.hasPermission(player, RegionPermission.REGION);
			}
			if(cancel) {
				if(event != null)
					event.setCancelled(true);
				if(player != null)
					MessageUtils.sendHotbarErrorMessage(player, RegionText.NO_PERMISSION_REGION.getValue());
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Handle the Interact event for a Region
	 * @param event The event
	 * @param interact The interact
	 * @param region The Region
	 * @param player The player
	 * @return true if the event has been cancelled, false otherwise
	 */
	public static boolean handleInteract(Cancellable event, EnumRegionInteract interact, Region region, Player player) {
		if(region != null) {
			if(!region.getInteract(interact) && !RegionUtils.hasPermission(player, region)) {
				event.setCancelled(true);
				MessageUtils.sendHotbarErrorMessage(player, RegionText.NO_PERMISSION_REGION.getValue());
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Shows a help page for a  player
	 * @param player The player
	 * @param page The page
	 */
	public static void printHelpPage(Player player, int page) {
		printHelpHeader(player, page);
		switch (page) {
		case 1:
		default:
			printHelpFor(player, "", RegionText.REGION_HELP_RG);
			printHelpFor(player, "save", RegionText.REGION_HELP_SAVE);
			printHelpFor(player, "info [region]", RegionText.REGION_HELP_INFO);
			printHelpFor(player, "delete [region]", RegionText.REGION_HELP_DELETE);
			printHelpFor(player, "name [name]", RegionText.REGION_HELP_NAME);
			break;
		case 2:
			printHelpFor(player, "list", RegionText.REGION_HELP_LIST);
			printHelpFor(player, "gamemode [gamemode]", RegionText.REGION_HELP_GAMEMODE);
			printHelpFor(player, "edit [region]", RegionText.REGION_HELP_EDIT);
			printHelpFor(player, "flag [subflag] [flag] [value]", RegionText.REGION_HELP_FLAG);
			printHelpFor(player, "add [role] [player] (region)", RegionText.REGION_HELP_ADD);
			break;
		case 3:
			printHelpFor(player, "remove [player] (region)", RegionText.REGION_HELP_REMOVE);
			printHelpFor(player, "setteleport [x] [y] [z]", RegionText.REGION_HELP_SET_TELEPORT);
			printHelpFor(player, "setspawn [x] [y] [z]", RegionText.REGION_HELP_SET_SPAWN);
			printHelpFor(player, "teleport [region]", RegionText.REGION_HELP_TELEPORT);
			printHelpFor(player, "spawn [region]", RegionText.REGION_HELP_SPAWN);
			break;
		case 4:
			printHelpFor(player, "priority [priority]", RegionText.REGION_HELP_PRIORITY);
			printHelpFor(player, "command [command]", RegionText.REGION_HELP_COMMAND);
			printHelpFor(player, "expand [direction] (blocks)", RegionText.REGION_HELP_EXPAND);
			printHelpFor(player, "here", RegionText.REGION_HELP_HERE);
			printHelpFor(player, "help (flag) (page)", RegionText.REGION_HELP_HELP);
			break;
		}
	}
	
	/**
	 * Shows a flag help page for a  player
	 * @param player The player
	 * @param page The page
	 */
	public static void printFlagHelpPage(Player player, int page) {
		printFlagHelpHeader(player, page);
		switch (page) {
		case 1:
		default:
			printFlagHelpFor(player, EnumRegionFlag.PLACE, RegionText.REGION_FLAG_HELP_PLACE);
			printFlagHelpFor(player, EnumRegionFlag.DESTROY, RegionText.REGION_FLAG_HELP_DESTROY);
			printFlagHelpFor(player, EnumRegionFlag.PVP, RegionText.REGION_FLAG_HELP_PVP);
			printFlagHelpFor(player, EnumRegionFlag.EXP_DROP, RegionText.REGION_FLAG_HELP_EXP_DROP);
			printFlagHelpFor(player, EnumRegionFlag.ITEM_DROP, RegionText.REGION_FLAG_HELP_ITEM_DROP);
			break;
		case 2:
			printFlagHelpFor(player, EnumRegionFlag.ENDERPEARL, RegionText.REGION_FLAG_HELP_ENDERPEARL);
			printFlagHelpFor(player, EnumRegionFlag.SLEEP, RegionText.REGION_FLAG_HELP_SLEEP);
			printFlagHelpFor(player, EnumRegionFlag.LIGHTER, RegionText.REGION_FLAG_HELP_LIGHTER);
			printFlagHelpFor(player, EnumRegionFlag.CHESTS, RegionText.REGION_FLAG_HELP_CHESTS);
			printFlagHelpFor(player, EnumRegionFlag.TRAPPED_CHESTS, RegionText.REGION_FLAG_HELP_TRAPPED_CHESTS);
			break;
		case 3:
			printFlagHelpFor(player, EnumRegionFlag.WATER_FLOW, RegionText.REGION_FLAG_HELP_WATER_FLOW);
			printFlagHelpFor(player, EnumRegionFlag.LAVA_FLOW, RegionText.REGION_FLAG_HELP_LAVA_FLOW);
			printFlagHelpFor(player, EnumRegionFlag.LEAF_DECAY, RegionText.REGION_FLAG_HELP_LEAF_DECAY);
			printFlagHelpFor(player, EnumRegionFlag.FIRE_SPREAD, RegionText.REGION_FLAG_HELP_FIRE_SPREAD);
			printFlagHelpFor(player, EnumRegionFlag.POTION_SPLASH, RegionText.REGION_FLAG_HELP_POTION_SPLASH);
			break;
		case 4:
			printFlagHelpFor(player, EnumRegionFlag.FALL_DAMAGE, RegionText.REGION_FLAG_HELP_FALL_DAMAGE);
			printFlagHelpFor(player, EnumRegionFlag.CAN_TP, RegionText.REGION_FLAG_HELP_CAN_TP);
			printFlagHelpFor(player, EnumRegionFlag.CAN_SPAWN, RegionText.REGION_FLAG_HELP_CAN_SPAWN);
			printFlagHelpFor(player, EnumRegionFlag.HUNGER, RegionText.REGION_FLAG_HELP_HUNGER);
			printFlagHelpFor(player, EnumRegionFlag.ENDER_CHESTS, RegionText.REGION_FLAG_HELP_ENDER_CHESTS);
			break;
		case 5:
			printFlagHelpFor(player, EnumRegionFlag.WALL_DAMAGE, RegionText.REGION_FLAG_HELP_WALL_DAMAGE);
			printFlagHelpFor(player, EnumRegionFlag.DROWN, RegionText.REGION_FLAG_HELP_DROWN);
			printFlagHelpFor(player, EnumRegionFlag.INVINCIBLE, RegionText.REGION_FLAG_HELP_INVINCIBLE);
			printFlagHelpFor(player, EnumRegionFlag.CACTUS_DAMAGE, RegionText.REGION_FLAG_HELP_CACTUS_DAMAGE);
			printFlagHelpFor(player, EnumRegionFlag.FIRE_DAMAGE, RegionText.REGION_FLAG_HELP_FIRE_DAMAGE);
			break;
		case 6:
			printFlagHelpFor(player, EnumRegionFlag.HIDE_LOCATIONS, RegionText.REGION_FLAG_HELP_HIDE_LOCATIONS);
			printFlagHelpFor(player, EnumRegionFlag.HIDE_FLAGS, RegionText.REGION_FLAG_HELP_HIDE_FLAGS);
			printFlagHelpFor(player, EnumRegionFlag.HIDE_MEMBERS, RegionText.REGION_FLAG_HELP_HIDE_MEMBERS);
			printFlagHelpFor(player, EnumRegionFlag.SEND_CHAT, RegionText.REGION_FLAG_HELP_SEND_CHAT);
			printFlagHelpFor(player, EnumRegionFlag.ENDERMAN_GRIEF, RegionText.REGION_FLAG_HELP_ENDERMAN_GRIEF);
			break;
		case 7:
			printFlagHelpFor(player, EnumRegionFlag.ENDER_DRAGON_BLOCK_DAMAGE,
					RegionText.REGION_FLAG_HELP_ENDER_DRAGON_BLOCK_DAMAGE);
			printFlagHelpFor(player, EnumRegionFlag.ENDER_DRAGON_BLOCK_DAMAGE,
					RegionText.REGION_FLAG_HELP_ENDER_DRAGON_BLOCK_DAMAGE);
			printFlagHelpFor(player, "interact", RegionText.REGION_FLAG_HELP_INTERACT);
			printFlagHelpFor(player, "vehiceplace", RegionText.REGION_FLAG_HELP_VEHICLE_PLACE);
			printFlagHelpFor(player, "vehicedestroy", RegionText.REGION_FLAG_HELP_VEHICLE_DESTROY);
			break;
		case 8:
			printFlagHelpFor(player, "explosiondamage", RegionText.REGION_FLAG_HELP_EXPLOSION_DAMAGE);
			printFlagHelpFor(player, "explosiondestroy", RegionText.REGION_FLAG_HELP_EXPLOSION_DESTROY);
			printFlagHelpFor(player, "mobspawn", RegionText.REGION_FLAG_HELP_MOB_SPAWN);
			printFlagHelpFor(player, "mobdamage", RegionText.REGION_FLAG_HELP_MOB_DAMAGE);
			printFlagHelpFor(player, "mobpve", RegionText.REGION_FLAG_HELP_MOB_PVE);
			break;
		case 9:
			printFlagHelpFor(player, EnumRegionFlag.ITEM_PICKUP, RegionText.REGION_FLAG_HELP_ITEM_PICKUP);
			printFlagHelpFor(player, EnumRegionFlag.OTHER_LIQUIDS_FLOW, RegionText.REGION_FLAG_HELP_OTHER_LIQUIDS_FLOW);
			printFlagHelpFor(player, EnumRegionFlag.HIDE_REGION, RegionText.REGION_FLAG_HELP_HIDE_REGION);
			printFlagHelpFor(player, EnumRegionFlag.ICE_MELT, RegionText.REGION_FLAG_HELP_ICE_MELT);
			printFlagHelpFor(player, EnumRegionFlag.VINES_GROWTH, RegionText.REGION_FLAG_HELP_VINES_GROWTH);
			break;
		case 10:
			printFlagHelpFor(player, EnumRegionFlag.EXIT, RegionText.REGION_FLAG_HELP_EXIT);
			printFlagHelpFor(player, EnumRegionFlag.ENTER, RegionText.REGION_FLAG_HELP_ENTER);
			break;
		}
	}

	/**
	 * Get the JSON file of a Region
	 * @param region The Region
	 * @return The JSON file of the Region
	 */
	public static File getFile(Region region) {
		if (region.getName().isEmpty()) {
			int index = 0;
			for (Region r : getAllRegions()) {
				if (r.getName().toLowerCase().startsWith("region"))
					index++;
			}
			region.setName("Region" + String.valueOf(index));
		}
		return new File((region.getType() == RegionType.LOCAL ? getRegionFolder() : getGlobalRegionFolder()) + "/"
				+ region.getName() + ".json");
	}

	/**
	 * Get the Local Regions folder
	 * @return The Local Regions folder
	 */
	public static File getRegionFolder() {
		return new File(getConfigFolder() + "/regions");
	}

	/**
	 * Get the Global Regions folder
	 * @return The Global Regions folder
	 */
	public static File getGlobalRegionFolder() {
		return new File(getConfigFolder() + "/globals");
	}
	
	/**
	 * Get the Old Local Regions folder
	 * @return The Old Local Regions folder
	 */
	public static File getOldRegionFolder() {
		return new File(getConfigFolder() + "/old/regions");
	}
	
	/**
	 * Get the Old Global Regions folder
	 * @return The Old Global Regions folder
	 */
	public static File getOldGlobalRegionFolder() {
		return new File(getConfigFolder() + "/old/globals");
	}

	/**
	 * Get the Configuration folder
	 * @return The Configuration folder
	 */
	public static String getConfigFolder() {
		return "config/universeguard/";
	}
}
