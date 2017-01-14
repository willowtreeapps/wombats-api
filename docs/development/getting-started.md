# Gettings started (Developers Guide)

- [Development Home](./)

### Architectural Overview

There are a number of different pieces that make up the Wombats platform.

1. Wombats Server / API (Current Repository)
1. Wombats web client
1. Wombats web canvas application

This guide is responsible for getting your development environment setup to work with the Wombats Server / API.

### Requirements

1. [Datomic](http://www.datomic.com/) Free.
   - [Download](https://my.datomic.com/downloads/free) Note: There is also a homebrew formula for Datomic.
1. [Boot](http://boot-clj.com/).
   - [Installation instructions](https://github.com/boot-clj/boot#install)

### Setup

1. Start datomic (Depending on your setup the start command may vary)
   - TODO: Provide transactor example in repo.
1. Start the boot repl
   - `$ boot repl`
1. Start the dev task
   - `boot.user => (boot "dev")`
1. Start the Wombats system
   - `boot.user => (reloaded.repl/go)`

### Workflow

Check out Stuart Sierra's [reloaded workflow](http://thinkrelevance.com/blog/2013/06/04/clojure-workflow-reloaded) to get an idea of how the development workflow for Wombats has been modeled.

#### reloaded commands

- `reloaded.repl/go`
- `reloaded.repl/reset`
- `reloaded.repl/reset-all`
- `reloaded.repl/start`
- `reloaded.repl/stop`
