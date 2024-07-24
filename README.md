# Steps to run this plugin
1. Clone this repo to a **directory** of your choice.
2. Open terminal and navigate to the **directory** where you cloned this repo.
3. run `mvn clean install`
4. run `mvn com.justas.deps:extract-deps:1.0-SNAPSHOT:extract-deps -Dproject-directory={YOUR-VALUE} -Dmps-directory={YOUR-VALUE} -Dinstall-dir={YOUR-VALUE}`

Parameters:
- project-directory = Directory of your MPS Project.
- mps-directory = Directory of where your MPS is (The directory should contain a plugins folder).
- install-dir = Directory of an installed plugin.

This plugin will detect missing plugins/dependencies based on your build.xml or plugin.xml files. But in order for it to add required plugins/dependencies
You must download/clone the plugin/dependency. This is what the **install-dir** parameter is asking for.
