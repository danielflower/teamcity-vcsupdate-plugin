Overview
========

VCSUpdate is a simple plugin for the TeamCity continuous integration server that
allows for an efficient and flexible method of triggering new builds. Rather
than poll the version control system (VCS) constantly, you can simply call a URL
on the build server to kick off a check for new sources.

You can configure your VCS (such as Subversion) to request the URL on your build
server whenever a developer commits a change. Typically, this would be done with
a post-commit hook and a tool such as wget or curl.

Aside from the obvious benefit of reducing the load on your VCS (by eliminating
constant checks for updates), VCSUpdate actually allows your build server to be
more responsive, since it can be notified immediately whenever the code changes.

Credits
=======

This project is a fork of the vcsupdate plugin created by Jon Vincent for TeamCity
7 support. The original project is hosted at http://code.google.com/p/vcsupdate/

Note that this plugin will be obsolete when http://youtrack.jetbrains.com/issue/TW-23077
is completed (probably TeamCity 8).

How to use vcsupdate
====================

Once you have the plugin installed on your TeamCity server, simply configure
your VCS server to request the vcsupdate.html page, and add parameters for each
of the VCS roots that should be queried for changes. For example:

http://my.build.server/vcsupdate.html?name=myvcsroot

In the example above, you would replace "my.build.server" with the URL of your
TeamCity build server, and you would replace "myvcsroot" with the name of the
VCS root that you've configured in TeamCity.

You can specify as many "name" parameters as you would like, separated by the
'&' character, or if you'd prefer to use VCS root IDs, you can specify "id"
parameters as well (ex: "id=3").

It's fine to use either GET or POST requests, but the behavior differs slightly
between the two. When you make a GET request, if you don't specify any VCS roots
(through "name" or "id" parameters), VCSUpdate won't do anything, and will
simply display some help text. However, when you make a POST request, if you
don't specify any VCS roots, VCSUpdate will check for updates on ALL of your
active VCS roots.

How to install vcsupdate
========================

Simply drop the [vcsupdate-2.0-teamcity-plugin.zip](http://danielflower.github.com/teamcity-vcsupdate-plugin/vcsupdate-2.0-teamcity-plugin.zip)
file into the plugins/ subdirectory of your TeamCity data directory (.BuildServer/plugins),
and restart TeamCity. Delete any existing vcsupdate*.zip files first.

Once you've installed VCSUpdate, you'll probably want to change your TeamCity
server configuration, and set the default VCS polling interval to something
much larger (a reasonable value is 43200 to check twice a day).

How to build vcsupdate
======================

If you'd prefer to build VCSUpdate yourself, you'll need Maven 2.0.6 or higher.
Just checkout the sources from googlecode, and run mvn assembly:assembly. That
will build the project and put the vcsupdate.zip file in the the target/
subdirectory. You can then copy it to your TeamCity plugins/ directory.
