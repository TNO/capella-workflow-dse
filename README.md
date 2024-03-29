# Capella Workflow 

A Capella addon for doing design space exploration (DSE) on Capella workflow models. To try it out, download a Capella release with this addon included from the [release page](https://github.com/TNO/capella-workflow-dse/releases) (Windows only). For more information on how to install and use see the [tutorial](tutorial/tutorial.pdf). This addon was presented on the Capella Days 2022 ([slides](https://www.slideshare.net/Obeo_corp/capelladays2022-thermofisher-esi-tno-a-method-for-quantitative-evaluation-of-functional-chains-supported-by-a-capella-addon) and [video](https://www.youtube.com/watch?v=BEYfcVq2glo&list=PLfrEYVpSGVLzbLqLJCohL_Cfgu8EzuXtr&index=2)).

![](images/dse.png)

The provided Capella release (from the [release page](https://github.com/TNO/capella-workflow-dse/releases)) contains the following:
- This addon
- [Capella 5.2.0](https://www.eclipse.org/capella/)
- [POOSL](https://www.poosl.org/)
- [Eclipse TRACE4CPS](https://projects.eclipse.org/projects/technology.trace4cps)
- [Rotalumis](https://www.es.ele.tue.nl/poosl/Tools/rotalumis/)
- [Property Values Management Tools (PVMT)](https://www.eclipse.org/capella/addons.html)

## Setup development environment
Below you will find instructions on how to setup a development environment on Windows. When the terminal needs to be used, use Windows Command Prompt, not Windows PowerShell.
1. First make sure Java 14 is installed and on your path. You can download it from [here](https://adoptopenjdk.net/releases.html?variant=openjdk14&jvmVariant=hotspot). The version can be checked by executing `java -version` in a terminal. Make sure that the `JAVA_HOME` environment variable points to the Java 14 installation directory. You can check this by executing `echo %JAVA_HOME%`. This should output something like: `C:\java\jdk-14.0.2` (note there is no `\bin` postfix).
1. Download and start the [Eclipse installer](https://www.eclipse.org/downloads/)
1. In the right-top click the "hamburger" -> "ADVANCED MODE..."
1. Select "Eclipse IDE for Eclipse Committers", set "Product Version" to "2020-06" and point "Java 1.8+ VM" to the location of Java 14, click "Next" twice.
1. Change "Installation folder name" to "capella-workflow-dse". If you want to change the installation folder, enable "Show all variables" and change accordingly. Click "Next" and "Finish"
1. Start Eclipse
1. Now we are going to clone this repository. For this you need to have [Git](https://git-scm.com/) installed. Open a terminal and navigate to the directory where you want to clone the repository. Clone it high in the filesystem structure to prevent build errors later (e.g. directly under `C:\`). In a terminal execute:
    ```
    git clone https://github.com/TNO/capella-workflow-dse.git
    cd capella-workflow-dse
    ```
1. Build the projects by executing:
    ```
    mvnw clean verify -f releng/nl.tno.capella.workflow.dse.target/pom.xml
    mvnw clean package
    ```
1. In Eclipse, press "File" -> "Import..." -> "General" -> "Existing Projects into Workspace", click "Next"
1. Click "Browse" next to "Select root directory" point it to the root of the cloned repository, click "Finish"
1. Open `nl.tno.capella.workflow.dse.target/platform.local.target`, click "Set as Active Target Platform". The "Load Target Platform" indicator will now appear in the right bottom, wait till it completes.
1. The development environment is now ready:
    - To launch the product, right click `nl.tno.capella.workflow.dse/Product.launch` -> "Run As" -> "Product"
    - To launch the tests, right click `nl.tno.capella.workflow.dse.test/Test.launch` -> "Run As" -> "Test". 
        - The logs may show an error: `ERROR => PROHIBITED SITUATION : There are many specifics mappings for the purpose <org.polarsys.capella.common.re>`, this can be safely ignored.
    - To launch the app, open a terminal, navigate to the root of the cloned repository and execute:
        ```
        cd plugins/nl.tno.capella.workflow.dse.app/app
        set PATH=%cd%\node;%PATH%
        npm start -- -- "../../../tests/nl.tno.capella.workflow.dse.test/model/3D Reconstruction"
        ```
        - Before doing this make sure you launched the DSE at least once via the product (`tests/nl.tno.capella.workflow.dse.test/model/3D Reconstruction/gen/dse` has to exist)

## Creating a new release
To create a new release, go to the [CI GitHub action](https://github.com/TNO/capella-workflow-dse/actions/workflows/ci.yml) -> Run workflow -> Fill in version -> Run workflow. Wait till build completes and add the [release notes](https://github.com/TNO/capella-workflow-dse/releases).

## License header
The Maven build uses [license-maven-plugin](https://github.com/mycila/license-maven-plugin) to determine if the correct license headers are used for source files. If the header is incorrect the build fails.

Handy commands:
- To only run the check execute: `mvn license:check -Dtycho.mode=maven`
- To automatically add/update execute: `mvn license:format -Dtycho.mode=maven`
