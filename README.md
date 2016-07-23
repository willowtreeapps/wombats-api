## WillowTree Wombats

![](https://circleci.com/gh/willowtreeapps/wombats-api.svg?style=shield&circle-token=:circle-token)

Wombats is multiplayer game inspired by [scalatron](https://scalatron.github.io/) written in [clojure](https://clojure.org/).

### How it works

Each player writes their own wombat code in clojure (other language support may become available in the future). Players then register it in an upcoming game and battle against other players.

### Setting up your development environment

#### Requirements

1. [leiningen](http://leiningen.org/)
1. [mongodb](https://docs.mongodb.com/)

#### Getting Started

##### Development Setup

1. [Register](https://github.com/settings/applications/new) a new GitHub OAuth Application.
1. Add the following environment variables.
   - **WOMBATS_GITHUB_CLIENT_ID** (GitHub Client ID)
   - **WOMBATS_GITHUB_CLIENT_SECRET** (GitHub Client Secret)
   - **WOMBATS_OAUTH_SIGNING_SECRET** (Random secret string)
   - **WOMBATS_WEB_CLIENT_URL** (Root URL that the user will be redirected to once Auth is complete)

1. (REQUIRED) Run `lein run` in root directory (builds project and runs server)
1. (REQUIRED - Unless using remote DB) If running DB locally, Run `mongod` to start MongoDB

##### Production Setup

Follow the steps for development setup.

1. Add the following to connect to a remote DB.
   - **WOMBATS_MONGOD_USER_NAME**
   - **WOMBATS_MONGOD_USER_PW**
   - **WOMBATS_MONGOD_HOST_LIST**
