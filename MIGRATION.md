# Migrating GumTuneClient to Minecraft 1.21.8

This document outlines the migration of GumTuneClient to support Minecraft 1.21.8 using Fabric.

## Overview

Due to the unavailability of the Essential multi-version framework in public repositories, this migration creates a standalone Fabric 1.21.8 version of the mod that coexists with the existing 1.8.9 Forge version.

## Project Structure

The project now contains:
- `/` - Original 1.8.9 Forge version (requires Essential multi-version framework)
- `/fabric-1.21.8` - New standalone Fabric 1.21.8 version

## Fabric 1.21.8 Setup

### Requirements
- Java 21
- Gradle 8.5+
- Fabric Loader 0.16.10+
- Minecraft 1.21.8

### Project Files Created

1. **gradle.properties** - Defines mod version, Minecraft version, and dependencies
2. **build.gradle** - Build configuration using Fabric Loom
3. **settings.gradle** - Gradle settings with Fabric maven repository
4. **fabric.mod.json** - Mod metadata for Fabric
5. **gumtuneclient.mixins.json** - Mixin configuration
6. **GumTuneClient.java** - Main mod entry point

### Building

To build the Fabric 1.21.8 version:

```bash
cd fabric-1.21.8
export JAVA_HOME=/path/to/java-21
./gradlew build
```

The compiled mod JAR will be in `fabric-1.21.8/build/libs/`

### Dependencies

The Fabric version requires:
- Fabric API 0.111.0+ for 1.21.8
- Fabric Loader 0.16.10+
- Java 21+

## Code Migration Notes

The codebase needs significant updates to work with Minecraft 1.21.8:

### API Changes from 1.8.9 to 1.21.8

1. **Package Renames**: Many Minecraft packages have been reorganized
2. **Mappings**: Must use Yarn mappings instead of MCP/SRG
3. **Event System**: Fabric uses a different event system than Forge
4. **Rendering**: Rendering code has changed significantly
5. **Networking**: Network packets use a different system

### Required Code Updates

1. Update all package imports to use modern Minecraft/Fabric packages
2. Convert Forge events to Fabric events
3. Update rendering code to use modern rendering APIs
4. Update networking code to use Fabric networking
5. Replace any Forge-specific APIs with Fabric equivalents

## Next Steps

1. **Test Build Environment**: Ensure Fabric Loom and dependencies can be downloaded
2. **Port Core Classes**: Migrate essential classes from the 1.8.9 version
3. **Update Mixins**: Convert mixins to use Yarn mappings
4. **Implement Fabric Events**: Replace Forge event handlers
5. **Test Functionality**: Verify all features work in 1.21.8

## Known Issues

- Essential multi-version framework is not accessible in the build environment
- Maven repositories may have connectivity issues in the current environment
- Full code migration requires manual porting of all mod features

## Alternative Approach

If the Essential framework becomes available or if a different multi-version solution is preferred:

1. Use Essential's preprocessor to maintain a single codebase
2. Add version-specific code using preprocessor comments
3. Build both versions from a single source tree

## Support

For issues with this migration, please refer to:
- [Fabric Documentation](https://fabricmc.net/wiki)
- [Fabric API Javadocs](https://maven.fabricmc.net/docs/fabric-api)
- [Minecraft 1.21.8 Changes](https://minecraft.wiki/w/Java_Edition_1.21.8)
