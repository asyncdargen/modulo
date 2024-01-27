# Modulo
Java library for simplest loading, reloading and unloading modules in runtime.
___
## Environment
Any **JRE** is supported. But for the redefinition module classes, instead of a total reload, it requires the use of [JBR](https://github.com/JetBrains/JetBrainsRuntime) with the **JVM** argument `-XX:+AllowEnhancedClassRedefinition`.
___
## Add to project
Connect to `jitpack.io` repository
```groovy
repositories {
    maven { url 'https://jitpack.io' }
}
```

Add dependency `com.github.asyncdargen:modulo:VERSION`
```groovy
dependencies {
    implementation 'com.github.asyncdargen:modulo:VERSION'
}
```
___

## Module loading
```java
//Use `Module ClassLoaderFactory.depend()` if the modules will depend on each other
Modulo modulo = Modulo.create(ModuleClassLoaderFactory.isolated()); 

//Loading
modulo.loadModule(ModuleLoader.loadInfo(Paths.get("module.jar")));

//Reload
modulo.reloadModule(ModuleLoader.loadInfo(Paths.get("module.jar")));

//Unload
modulo.unloadModule("example");
```
___

## Module example
```java
class ExampleModule implements ModuleEntrypoint {

    @Override
    public void enable() {
        System.out.println("[Test] Enabled");
    }

    @Override
    public void reload() {
        System.out.println("[Test] Reloaded");
    }

    @Override
    public void disable() {
        System.out.println("[Test] Disabled");
    }

}
```
`module.properties`:
```properties
name=example
entrypoint=path.to.ExampleModule

#Using kotlin object as ModuleEntrypoint
kotlin-object-entrypoint=false
```
For depend classloader strategy
```properties
#By default all modules accessed to all modules
isolate=false
#Allow access in all modules
force-depend=false

#Allow access if isolated module ([, ]), example: depends=test, test2,example-depend
#depends=example-depend 
```
___

## BuiltIn Loaders
Directory watch loader
```java
Modulo modulo = //...;
ModuleFilesLoader.watch(module, "modules/");
```