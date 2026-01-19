# ADR-0007: Choose Gradle as Build tool

## Status
Accepted

## Context
We need a build tool to help use structure, build and compile our project. We will be using a multi-module approache and gradle is known for its simplicity, easy-of-use and modern approach to build projects. 

Options considered:
1. **Maven** - Mature build tool, XML-based, widely used in enterprise
2. **Gradle** - Modern build tool, Groovy/Kotlin DSL, flexible and powerful
3. **Bazel** - Google's build tool, excellent for very large monorepos
4. **Ant** - Legacy build tool, XML-based, manual dependency management

## Decision
We will use **Gradle** as the build tool.

Key factors:
- **Widespread use**: Many organization are using it on a day to day basis from startup to big firm. This makes it a reliable tool with massive support. 
- **Real-world alignment**: In reality I've been employed in multiple places that used Gradle as their build tool
- **Mature ecosystem**: Many plugin available and out-of-the box features
- **Learning objectives**: Opportunity to apply mono-repos principles which is not something I usually do for the back-end

## Consequences

### Positive
- **Multi-module support**: Native support for multi-module projects with clean dependency management
- **Build performance**: Incremental builds and build cache significantly speed up repeated builds
- **Kotlin DSL**: Type-safe build scripts with IDE support (autocomplete, refactoring)
- **Flexibility**: Highly customizable and extensible through plugins and custom tasks
- **Dependency management**: Sophisticated dependency resolution with version catalogs
- **Plugin ecosystem**: Rich ecosystem of plugins for testing, code quality, containerization
- **Industry standard**: Widely adopted in Android, Spring Boot, and modern Java projects
- **Parallel execution**: Can build multiple modules in parallel automatically

### Negative
- **Learning curve**: More complex than Maven, especially for advanced features
- **Build script complexity**: Can become complicated if not well-organized
- **Debugging builds**: Harder to debug build issues compared to simpler tools
- **IDE integration**: Sometimes requires daemon restarts or cache invalidation
- **Build time (initial)**: First build can be slow, though cache helps subsequent builds
- **Version compatibility**: Plugin version compatibility can sometimes be tricky

### Mitigation Strategies
- Use Gradle wrapper to ensure consistent Gradle version across environments
- Leverage build cache and daemon for faster builds
- Keep build scripts organized with convention plugins for shared configuration
- Use version catalogs to centralize dependency management
- Enable parallel builds and configure memory appropriately
- Document custom build logic clearly
- Use `./gradlew --scan` for build insights and debugging

## References
- [Gradle Multi-Module Projects](https://docs.gradle.org/current/userguide/multi_project_builds.html)
- [Gradle Build Cache](https://docs.gradle.org/current/userguide/build_cache.html)
- [Gradle Kotlin DSL](https://docs.gradle.org/current/userguide/kotlin_dsl.html)
- [Structuring Large Projects](https://docs.gradle.org/current/userguide/structuring_software_products.html)
