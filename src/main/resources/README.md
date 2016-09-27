# Including Property File and Configuration Files for Testing

The default properties file must be included in the WAR file, to list the properties
required by this service (and reasonable defaults if appropriate).

The deployment/context-specific properties should not be included in the WAR file,
however. This is why the `.gitignore` file should specifically list configuration files to be
excluded from this directory, to avoid them ending up in a WAR file.

In this case, the file `application-configuration-data.properties` should be in
`.gitignore` and the file should not be present in the source tree in Git.  

For testing and development purposes it may be convenient, however, to include the
properties file from `src/test/resources/application-configuration-data.properties`
and the corresponding configuration file `sample.json` here as well.

You can safely copy them to this directory for testing, as Git will ignore these 
files next to time to wish to add or commit something to the source repository.
