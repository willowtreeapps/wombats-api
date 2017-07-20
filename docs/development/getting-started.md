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
   - [Download](https://my.datomic.com/downloads/free) Note: There is also a homebrew formula for Datomic (`brew install datomic`)
1. [Boot](http://boot-clj.com/).
   - [Installation instructions](https://github.com/boot-clj/boot#install)

### Setup for Development

1. Start datomic (Depending on your setup the start command may vary)
   - Create a file called `free-transactor.properties` in the datomic directory with the following properties
   ```
   protocol=free
   host=localhost
   port=4334
   memory-index-threshold=32m
   memory-index-max=256m
   object-cache-max=128m
   data-dir=~/local/datomic/data # or any dir
   log-dir=~/local/datomic/log # or any dir

   ```
   - Run datomic with the following command:
   ```
   datomic_dir/bin/transactor datomic_dir/free-transactor.properties
   ```
1. Create a GitHub OAuth Application [here](https://github.com/settings/applications/new).
   - Name can be anything
   - Homepage URL is `localhost:3449`
   - Callback URL is `http://localhost:8888/api/v1/auth/github/callback`
   - Once the application is created, keep the page open, you will need the client-id and the client-secret for the next step.

1. Make a copy of the config_empty.edn file located at `wombats-api/config/config-empty.edn` named `config.edn`
    - Place the file in either `~/.wombats/config.edn` or in `wombats-api/config/config.edn`. The home directory `.wombats` location will override the file in `wombats-api`.
    - Replace the Github Client ID and Github Client Secret with your Client ID and Secret.

1. Start the boot repl
    - `$ boot repl`
1. Seed the database
    - `boot.user => (refresh)`
    - Type "Yes" to confirm that you want to refresh the database.
1. Start the Wombats system
   - `boot.user => (reloaded.repl/go)`

### Setup for Production / QA / Online Database Development

1. Create account with datomic to get access to a download key for the Pro Starter version.
   - Add two enviroment variables to allow Datomic to download when running the app with Boot.
        - `DATOMIC_USERNAME=YourEmail@example.com`
        - `DATOMIC_PASSWORD=YourDatomicPassword`
1. To run on another environment than local, create an environment variable called `WOMBATS_ENV`.

    Possible values for `WOMBATS_ENV` are:
    - `dev` (local)
    - `dev-ddb` (development database)
    - `qa-ddb` (qa database)
    - `prod-ddb` (production database)
1. Create a GitHub OAuth Application [here](https://github.com/settings/applications/new).
   - Name can be anything
   - Homepage URL is `localhost:3449`
   - Callback URL is `http://localhost:8888/api/v1/auth/github/callback`
   - Once the application is created, keep the page open, you will need the client-id and the client-secret for the next step.

1. Make a copy of the config_empty.edn file located at `wombats-api/config/config-empty.edn` named `config.edn`
    - Place the file in either `~/.wombats/config.edn` or in `wombats-api/config/config.edn`. The home directory `.wombats` location will override the file in `wombats-api`.
    - Replace the Github Client ID and Github Client Secret with your Client ID and Secret.
    - `:aws nil` allows the program to run without AWS Lambda
    - For Lamda support the `:aws` object should look like this:
    ```
    :aws {:access-key-id “YourAWSAccessKeyId”
    :secret-key “YourAWSSecretKey”}
    ```
1. Start the boot repl
    - `$ boot repl`
1. Start the Wombats system
   - `boot.user => (reloaded.repl/go)`

### Building the API
    - Building the Wombats API is very easy, just run `boot build`.
    - This option must be run with a flag that is not `dev` or `dev-ddb` for the WOMBATS_ENV environment variable.
    - Once compiled, the jar is placed in `wombats-api/target/wombats.jar`.
    - WOMBATS_ENV variable must be set in the launch options to one of the values specified above.
    - Run the API with a command like this `WOMBATS_ENV=env java -jar target/wombats.jar`.

### Workflow

Check out Stuart Sierra's [reloaded workflow](http://thinkrelevance.com/blog/2013/06/04/clojure-workflow-reloaded) to get an idea of how the development workflow for Wombats has been modeled.

#### reloaded commands

- `reloaded.repl/go`
- `reloaded.repl/reset`
- `reloaded.repl/reset-all`
- `reloaded.repl/start`
- `reloaded.repl/stop`

#### boot tasks (run from repl)

- `seed`
- `refresh`
- `refresh-db-functions`
- `delete`
