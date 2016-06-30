## WillowTree Battlebots

![](https://circleci.com/gh/willowtreeapps/battlebots.svg?style=shield&circle-token=:circle-token)

Battlebots is multiplayer game inspired by [scalatron](https://scalatron.github.io/) written in [clojure](https://clojure.org/).

### How it works

Each player writes their own bot in clojure (other language support may become available in the future). Players then register it in an upcoming game and battle against other bots.

### Setting up your development environment

#### Requirements

1. [leiningen](http://leiningen.org/)
1. [mongodb](https://docs.mongodb.com/)

#### Getting Started (Development)

1. For development, [Register](https://github.com/settings/applications/new) a new GitHub OAuth Application NOTE: This step will be removed when we get a WillowTree GitHub App.
1. (REQUIRED) Add the following environment variables.
   - **BATTLEBOTS_GITHUB_CLIENT_ID** (GitHub Client ID)
   - **BATTLEBOTS_GITHUB_CLIENT_SECRET** (GitHub Client Secret)
   - **BATTLEBOTS_OAUTH_SIGNING_SECRET** (Random secret string)
1. (OPTIONAL) If running a remote database, add the following environment variables. (This is required if running in production.
   - **BATTLEBOTS_MONGOD_USER_NAME**
   - **BATTLEBOTS_MONGOD_USER_PW**
   - **BATTLEBOTS_MONGOD_HOST_LIST**
1. (REQUIRED) Run `lein run` in root directory (builds project and runs server)
1. (REQUIRED - CLIENT DEV ONLY) Run `lein figwheel` in root directory (compiles clojurescript and watches)
1. (REQUIRED - CLIENT DEV ONLY) Run `lein less auto` in root directory (watches styles)
1. (REQUIRED - Unless using remote DB) If running DB locally, Run `mongod` to start MongoDB
