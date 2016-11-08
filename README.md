# apjava-lab1
Assignment 1 for the course Applications development in Java

## Compile to jar
```
javac -cp junit-4.12.jar:hamcrest-core-1.3.jar —d out src/*.java tests/*.java
jar -cvfm lab1.jar src/META-INF/MANIFEST.MF out/*.class 
```
