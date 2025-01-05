package keystrokesmod.utility.command.impl;

import keystrokesmod.Raven;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.command.Command;
import keystrokesmod.utility.profile.Profile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Profiles extends Command {
    public Profiles() {
        super("profiles", new String[]{"profile", "p"});
    }

    @Override
    public void onExecute(String[] args) {
        if (args.length < 2) {
            chat("&7Usage:");
            chat("&7.profiles save <name> &8- &7Save current settings as a profile");
            chat("&7.profiles load <name> &8- &7Load a profile");
            chat("&7.profiles delete <name> &8- &7Delete a profile");
            chat("&7.profiles rename <oldname> <newname> &8- &7Rename a profile");
            chat("&7.profiles list &8- &7List all profiles");
            return;
        }

        String subCommand = args[1].toLowerCase();

        switch(subCommand) {
            case "save":
                if (args.length < 3) {
                    chat("&cUsage: .profiles save <name>");
                    return;
                }
                String saveName = args[2];
                Profile newProfile = new Profile(saveName, 0);
                Raven.profileManager.profiles.add(newProfile);  // add the profile to the list otherwise it fails to load the list... like a bitch
                Raven.profileManager.saveProfile(newProfile);
                chat("&7Profile &b" + saveName + " &7has been saved.");
                break;

            case "load":
                if (args.length < 3) {
                    chat("&cUsage: .profiles load <name>");
                    return;
                }
                String loadName = args[2];
                if (Raven.profileManager.getProfile(loadName) == null) {
                    chat("&cProfile &b" + loadName + " &cdoes not exist!");
                    return;
                }
                Raven.profileManager.loadProfile(loadName);
                chat("&7Profile &b" + loadName + " &7has been loaded.");
                break;

            case "delete":
                if (args.length < 3) {
                    chat("&cUsage: .profiles delete <name>");
                    return;
                }
                String deleteName = args[2];
                if (Raven.profileManager.getProfile(deleteName) == null) {
                    chat("&cProfile &b" + deleteName + " &cdoes not exist!");
                    return;
                }
                Raven.profileManager.deleteProfile(deleteName);
                chat("&7Profile &b" + deleteName + " &7has been deleted.");
                break;

            case "rename":
                if (args.length < 4) {
                    chat("&cUsage: .profiles rename <oldname> <newname>");
                    return;
                }
                String oldName = args[2];
                String newName = args[3];
                Profile oldProfile = Raven.profileManager.getProfile(oldName);
                if (oldProfile == null) {
                    chat("&cProfile &b" + oldName + " &cdoes not exist!");
                    return;
                }
                if (Raven.profileManager.getProfile(newName) != null) {
                    chat("&cProfile &b" + newName + " &calready exists!");
                    return;
                }
                Profile renamedProfile = new Profile(newName, oldProfile.getBind()); // save this to that pls
                Raven.profileManager.saveProfile(renamedProfile);
                Raven.profileManager.deleteProfile(oldName); // delete old shit (i think thats needed)
                Raven.profileManager.loadProfiles(); // ok so fun fact!! you need to load them again (i think??)
                chat("&7Profile &b" + oldName + " &7has been renamed to &b" + newName);
                break;

            case "list":
                List<Profile> profiles = Raven.profileManager.profiles;
                if (profiles.isEmpty()) {
                    chat("&cNo profiles found!");
                    return;
                }
                chat("&7Profiles (&b" + profiles.size() + "&7):");
                for (Profile profile : profiles) {
                    chat("&8- &b" + profile.getName() +
                            (profile == Raven.currentProfile ? " &7(Current)" : ""));
                }
                break;

            default:
                chat("&cUnknown subcommand. Use .profiles for help.");
                break;
        }
    }

    @Override
    public List<String> tabComplete(String[] args) {
        if (args.length == 2) {
            return filterStartingWith(args[1], Arrays.asList("save", "load", "delete", "rename", "list"));
        } else if (args.length == 3 && !args[1].equalsIgnoreCase("save") && !args[1].equalsIgnoreCase("list")) {
            List<String> profileNames = new ArrayList<>();
            for (Profile profile : Raven.profileManager.profiles) {
                profileNames.add(profile.getName());
            }
            return filterStartingWith(args[2], profileNames);
        }
        return new ArrayList<>();
    }

    private List<String> filterStartingWith(String prefix, List<String> options) {
        List<String> filtered = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(prefix.toLowerCase())) {
                filtered.add(option);
            }
        }
        return filtered;
    }
}