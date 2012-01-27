## Campfire notifier plugin for Jenkins

This is a fork of the plugin developed by Jens Lukowski. More information about
the original plugin is available from the [Hudson
wiki](http://wiki.hudson-ci.org/display/HUDSON/Campfire+Plugin) and [this blog
post from
Jens](http://schneide.wordpress.com/2009/10/26/a-campfire-plugin-for-hudson/).

Development of the original plugin seemed to stall, so I forked it to address a
number of issues and add some extra features...

* Refactored the code to fix a number of null pointer exceptions.
* Moved from per-job to global config.
* Fixed issues with configuration details being lost after a restart.
* Tidied up jelly view for configuration form and added help files for each
  field.
* Added a link to the build in notifications sent to campfire.

Other features have since been added including...

* Support for campfire accounts with SSL enabled, added by Joshua Krall.
* A smart notification feature, added by Brad Greenlee, which disables
  success notifications unless the previous build was unsuccessful.
* Play sounds on build success and failure, added by Henry Poydar.
* Build notifications now include commit info.
* Room to which notifications are sent can be customised per-project.

Note: The plugin code is a bit of a mess, partly just because I don't have a
lot of Java experience, but also because I simply haven't got the time to tidy
it up. It does work though, and we use it daily without any trouble.

Update: The plugin code is less of a mess that it was, thanks to Dante Briones.
Yay, thanks Dante, and welcome aboard!

### Installation

A pre-built hpi file is provided in the downloads area on github. You should be
able to upload this to your Jenkins instance via the advanced tab of the plugin
manager. This build should be based on the latest stable/tagged release. To get
the latest development version, you should build from source...

### Building from source

You'll need to have JDK 6 and maven installed to build the plugin from source.
This should be as simple as asking your package manager to install maven, e.g.

    brew install maven

Then clone the repository and build the package

    git clone git://github.com/thickpaddy/jenkins_campfire_plugin.git
    cd jenkins_campfire_plugin
    mvn package

When the build has completed, you'll find a campfire.hpi file in the target
directory, which needs to be uploaded to your Jenkins installation. If you
already have a campfire plugin installed, you need to delete it first, e.g.

    rm -rf /var/lib/jenkins/plugins/campfire*

Then either use the advanced tab of the plugin manager to upload the hpi file or
just copy it to the plugins directory, e.g.

    cp target/campfire.hpi /var/lib/jenkins/plugins/

Finally, restart jenkins (note: not reload configuration, restart the jenkins
daemon).

### Troubleshooting

If you run into problems building the plugin with Maven, make sure Maven is
finding the right jdk...

run mvn --version and check the output, if it's not finding jdk 6, make sure you
have jdk 6 installed and make sure that the current jdk symlink points at
version 6.  On OSX, check that
/System/Library/Frameworks/JavaVM.framework/Versions/1.6 exists and that
/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK points to it.
If not, remove the CurrentJDK symlink and re-create it, pointing at the 1.6
directory. Other *nix users may run into similar issues, the solution should be
the same, just with different paths.

If you get HttpClient or WebClient exceptions, that probably means you've got
some configuration setting wrong (while there is some validation of
configuration settings, it's far from extensive).
