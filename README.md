# TomatenConfig

TomatenConfig is a parser for configuration files written in Java.
Currently, TomatenConfig supports JSON and TOML configuration formats.

## Usage

1. Create the configuration file you want to parse.

```json
{
    "title": "Example configuration",
    "tags": ["config", "json", "toml"],
    "database": {
        "ip": "192.168.1.1",
        "user": "db",
        "timeout": 10.0
    }
    "settings": {
        "cache": true
    }
}
```

2. Load the configuration file. The format of the configuration (JSON or TOML) is automatically detected based on the file extension.

```java
Config config = TomatenConfig.load(Config::new, Paths.get(pathToConfig));
```

3. Read content from the `Config` object.

```java
String title = config.getString("title").orError();
String[] tags = config.getList("tags").orError().stream()
        .map(c -> c.getString().orError())
        .toArray(String[]::new);

Config database = config.getObject("database").orError();
String dbIp = database.getString("ip").orError();
String dbUser = database.getString("user").orError();
double dbTimeout = database.getDouble("timeout").orDefault(5.0);

boolean cache = config.getBoolean("settings.cache").orDefault(false);
```

For more information, see the [Docs](https://docs.tomaten.dev/software/tomatenconfig/) and the [JavaDoc](https://docs.tomaten.dev/javadoc/TomatenConfig/).

## Parser Notes

- The JSON parser also allows non-strict JSON input. That means, for example, that keys don't need to be quoted.
- The TOML parser supports the TOML v1.1 preview features and is less strict than the specification in some situations. For example, the parser allows multiple commas in arrays. However, all *valid* [TOML v1.0.0](https://toml.io/en/v1.0.0) inputs should be parsed correctly.

## How to install

You can install TomatenConfig using the [TomatenPack](https://gitlab.tomaten.dev/Basic7x7/tomatenpack) package.

```sh
tomatenpack install TomatenConfig
```

You can also download the JAR file from the [Releases](https://gitlab.tomaten.dev/Basic7x7/tomatenconfig/-/releases) page directly.

## Dependencies

Java 8+ is required.

| Dependency | Tested Version | Required |
| ------ | ------ | ------ |
| [CompilerLib](https://gitlab.tomaten.dev/Basic7x7/compilerlib) | 1.4.4 | yes |
| [TomatenJSON](https://gitlab.tomaten.dev/Lukas/tomatenjson) | 1.5 | yes |
| [TomatenUtil](https://gitlab.tomaten.dev/Basic7x7/tomatenutil) | 1.7 | yes |
