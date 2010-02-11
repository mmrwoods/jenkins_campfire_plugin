## Campfire plugin for Hudson

This is a fork of the plugin developed by Jens Lukowski. More information about the original plugin is available from the [Hudson wiki](http://wiki.hudson-ci.org/display/HUDSON/Campfire+Plugin) and [this blog post from Jens](http://schneide.wordpress.com/2009/10/26/a-campfire-plugin-for-hudson/).

This fork addresses a number of issues with the original plugin, and adds some extra features...

* Refactored the code to fix a number of null pointer exceptions.
* Moved from per-job to global config. 
* Fixed issues with configuration details being lost after a hudson restart.
* Tidied up jelly view for configuration form and added help files for each field.
* Added a link to the build in notifications sent to campfire.

### Installation 

You'll need to have JDK 6 and maven2 installed to build the plugin. On OSX with macports this is as simple as...

    sudo port install maven2

Then clone the repository and build the package

    git clone git@github.com:jgp/hudson_campfire_plugin.git
    cd hudson_campfire_plugin
    mvn package

When the build has completed, you'll have a .hpi file available which needs to be uploaded to your Hudson installation. If you already have a campfire plugin installed, you need to delete it first, e.g.

    rm -rf /var/lib/hudson/plugins/campfire*

Then use the advanced tab of the plugin manager to upload the hpi file. Finally, restart hudson (note: not reload configuration, restart the hudson daemon).
