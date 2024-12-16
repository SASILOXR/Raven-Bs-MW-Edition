package keystrokesmod.utility.command.impl;

import keystrokesmod.utility.command.Command;

public class Help extends Command {
    public Help() {
        super("help");
    }

    @Override
    public void onExecute(String[] args) {
        chat("&7[&fhelp&7] Chat commands - &dGeneral");
        chat(" &b.ign/name &7Copy your username.");
        chat("&7[&fhelp&7] Chat commands - &dModules");
        chat(" &b.cname [name] &7Set name hider name.");
        chat(" &b.binds (key) &7List module binds.");
    }
}
