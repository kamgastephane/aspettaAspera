# RandomDownloader

Just another file downloader

## Getting Started
These instructions will get you a copy of the project up and running on your local machine for development and testing purposes

### Prerequisites

To run properly, the following components are required

* [JAVA JDK v.1.8](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* [Apache Maven](https://maven.apache.org/install.html) recent release

### Installing

From now on, assume you are running on an environment with both JAVA and MAVEN properly installed and configured
The following test was done on a UNIX environment. Some changes may apply for Windows.

First thing first, you should download the project and save it inside a known folder
We will then navigate to this folder using the command line
Let suppose the files of the project are saved inside our disk at: _download/project/agoda_

```
$ cd 
$ cd download/project/agoda
```

Our command line  should be inside our project folder.

### Build

The project is built using maven. The built process can be triggered using the following command

 ```
 $ mvn clean  test compile
 ```
The previous command automatically ran the test and build our source files

## Running the tests

### Unit tests

Inside the project folder at _download/project/agoda_, you can launch the test suite as a standalone using the command
 ```
 $ mvn test
 ```
 
### Integration tests
This test require access to the file system as well as access to the network
```
 mvn integration-test -DskipTests=false
```

Losgs file containing the result of the test suite can be found at 
 inside the **target** folder located inside our disk at path:  _download/project/agoda/target_

##running the app

To run the app, we should generate a JAR file
This can be done using the following command

 ```
 $  mvn clean compile assembly:single
 ```
 inside the **target** folder located at path _download/project/agoda/target_
 you can find the new generated JAR file named: _downloader-1.0-SNAPSHOT.jar_
 The application can be launched in two different ways:
 
      
#### Launching the program 

We should provide to the application the path to the configuration file with the alias -c.
An example of a configuration file can be found inside the project with the name config.yaml
Finally we can then provide a list of resource we would like to download.
Currently HTTP is supported, FTP support is experimental
e.g.
 ```
 $ java -jar downloader-1.0-SNAPSHOT.jar -c path/to/my/config.yaml http://speedtest.tele2.net/100MB.zip
 ```
 To run the program in verbose mode, just include the additional option -v as
 ```
 $ java -jar downloader-1.0-SNAPSHOT.jar -c path/to/my/config.yaml http://speedtest.tele2.net/100MB.zip -v
 ```
## Deployment CI/CD


## Built With

* [Intellij IDEA](https://www.jetbrains.com/idea/) - The IDE used
* [Apache Maven 3.6.0](https://maven.apache.org/) - Dependency Management
* [Junit](https://junit.org/junit5/)  - The testing framework used

## Versioning

V1.0

## Authors

* **Stephane Kamga**


## Acknowledgments

* Hat tip to StackOverflow

