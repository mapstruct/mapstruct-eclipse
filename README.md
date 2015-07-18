# mapstruct-eclipse
An Eclipse plug-in for working with MapStruct

Eclipse update site for latest snapshot: https://mapstruct.ci.cloudbees.com/job/mapstruct-eclipse/lastSuccessfulBuild/artifact/org.mapstruct.eclipse.repository/target/repository/

## Current Features

### Code-Completions

* Completion of `target` and `source` properties in `@Mapping` annotation for bean mappings and for enum mappings
* Completion of `componentModel` values in `@Mapper` annotation

### Quick-Fixes

* Quick-Fixes for error/warning message `"Unmapped target property: ..."`:
  * _Ignore unmapped target property_ adds `@Mapping( target = "prop", ignore = true )` to the method
* Quick-Fixes for error message `"Can't map property X prop to Y prop. Consider to declare/implement a mapping method ..."`:
  * _Add method: Y toY(X prop)_ adds an appropriate method declaration to the mapper.
  * _Ignore unmapped target property_ adds `@Mapping( target = "prop", ignore = true )` to the method.

## Links

* [Homepage](http://mapstruct.org)
* [Source code](https://github.com/mapstruct/mapstruct-eclipse/)
* [Issue tracker](https://github.com/mapstruct/mapstruct-eclipse/issues)
* [User group](https://groups.google.com/forum/?hl=en#!forum/mapstruct-users)
* [CI build](https://mapstruct.ci.cloudbees.com/)

<div style="float: right">
    <a href="https://mapstruct.ci.cloudbees.com/"><img src="http://www.cloudbees.com/sites/default/files/Button-Built-on-CB-1.png"/></a>
</div>

