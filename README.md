# capsule-maven-13

This is a reduced program that reproduces https://github.com/puniverse/capsule-maven/issues/14: it uses Aether just like `capsule-maven`does, including a `ConflictResolver` that embeds `NearestVersionSelector`, in order to retrieve the following dependencies:

```
    compile 'ch.qos.logback:logback-classic:1.1.6' // Depends on slf4j-api 1.7.18
    compile 'org.slf4j:slf4j-simple:1.7.19'        // Depends on slf4j-api 1.7.19
```

The conflict between `org.slf4j:slf4j-api:jar:1.7.18` and `org.slf4j:slf4j-api:jar:1.7.19` seems not to be automatically resolved towards `1.7.19`. Indeed, `gradlew run` yields:

```
Resolving: [ch.qos.logback:logback-classic:jar:1.1.6 (compile), org.slf4j:slf4j-simple:jar:1.7.19 (compile)]

12:06:07.560 [main] DEBUG org.eclipse.aether.internal.impl.DefaultLocalRepositoryProvider - Using manager EnhancedLocalRepositoryManager with priority 10.0 for /home/fabio/.m2/repository
12:06:07.855 [main] DEBUG org.eclipse.aether.internal.impl.DefaultDependencyCollector - Dependency collection stats: {ConflictMarker.analyzeTime=1, ConflictMarker.markTime=2, ConflictMarker.nodeCount=6, ConflictIdSorter.graphTime=1, ConflictIdSorter.topsortTime=0, ConflictIdSorter.conflictIdCount=4, ConflictIdSorter.conflictIdCycleCount=0, ConflictResolver.totalTime=5, ConflictResolver.conflictItemCount=5, DefaultDependencyCollector.collectTime=266, DefaultDependencyCollector.transformTime=12}

Result: [ch.qos.logback:logback-classic:jar:1.1.6 < https://repo1.maven.org/maven2/ (https://repo1.maven.org/maven2/, default, releases+snapshots), ch.qos.logback:logback-core:jar:1.1.6 < https://repo1.maven.org/maven2/ (https://repo1.maven.org/maven2/, default, releases+snapshots), org.slf4j:slf4j-api:jar:1.7.18 < https://repo1.maven.org/maven2/ (https://repo1.maven.org/maven2/, default, releases+snapshots), org.slf4j:slf4j-simple:jar:1.7.19 < https://repo1.maven.org/maven2/ (https://repo1.maven.org/maven2/, default, releases+snapshots), org.slf4j:slf4j-api:jar:1.7.19 < https://repo1.maven.org/maven2/ (https://repo1.maven.org/maven2/, default, releases+snapshots)]
```
