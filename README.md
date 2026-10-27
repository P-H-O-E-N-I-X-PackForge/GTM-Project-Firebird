# GTM: Project Firebird.
A GregTech:Modern fork focused on being driven by packdevs for packdevs. Most changes will be related to configs for currently hard coded balancing ideals and code extensibility.

### BaseGTM [Wiki](https://gregtechceu.github.io/GregTech-Modern/)

## Developers

To add GTCEu: Modern (GTM) to your project as a dependency, add the following to your `build.gradle`:
```groovy
repositories {
    maven {
        name = 'GTCEu Maven'
        url = 'https://maven.gtceu.com'
        content {
            includeGroup 'com.gregtechceu.gtceu'
        }
    }
}
```
Then, you can add it as a dependency, with `${mc_version}` being your Minecraft version target and `${gtm_version}` being the version of GTM you want to use.
```groovy
dependencies {
	// Forge (see below block as well if you use Forge Gradle)
	implementation fg.deobf("com.gregtechceu.gtceu:gtceu-${mc_version}:${gtm_version}")

	// NeoForge
	implementation "com.gregtechceu.gtceu:gtceu-${mc_version}:${gtm_version}"

	// Architectury
	modImplementation "com.gregtechceu.gtceu:gtceu-${mc_version}:${gtm_version}"
}
```

### IDE Requirements (when using IntelliJ IDEA)

For contributing to this mod, the [Lombok plugin](https://plugins.jetbrains.com/plugin/6317-lombok) for IntelliJ IDEA is strictly required.  
Additionally, the [Minecraft Development plugin](https://plugins.jetbrains.com/plugin/8327-minecraft-development) is recommended.


## Credited Works
Check out the original GTM here. We do not claim to be any better than base GTM and if you have any collaborations that you feel might better fit base GTM by all means go help them out!
