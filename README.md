## Wombats

![](https://circleci.com/gh/willowtreeapps/wombats-api.svg?style=shield&circle-token=:circle-token)

![wombat_git](https://cloud.githubusercontent.com/assets/4649439/17083937/59e5a5f0-517d-11e6-92a2-976aee52d95c.png)

### What is Wombats?

Wombats is multiplayer game where players write code to control a wombat in an arena filled with obstacles, enemies, and other wombats.
Wombats was inspired by [scalatron](https://scalatron.github.io/) and is written in [clojure](https://clojure.org/).

### How it works

1. Visit wombats.willowtreemobile.com and sign up. (Sign up will ask for GitHub read only permission. This is used for user authenication and wombat registration / tracking)
1. Register a wombat under your user settings. (Don't have a wombat? Check out "Writing your first Wombat")
1. Join a game to play!

### Writing your first Wombat

Disclaimer... Very little research on wombats was conducted prior to the development of Wombats. Enjoy.

Wombats are not that smart and they need your help to pretty much do anything. Really... anything. In a world where wombats must fight to survive while navigating the treacherous terrain around them, it is paramount that you do your part and support a wombat. So how can you help?

While it is common knowledge that wombats are excellent at following directions and prefer their directions in bytecode, it is a lesser known fact that they crave bytecode compiled from functional Lisp languages such as Clojure. But how do we get commands to our wombats from the comfort of our familiar world?! Well lucky for you, wombats regularly scour GitHub in search of code that will aid them in their mission to survive in a dangerous world.

##### Here's how you can help a wombat find your code.

1. Create a GitHub Repository with the name of your wombat.
1. Add a `bot.clj` file to the root of the repository. (This will be updated to `wombat.clj` soon)
1. Check out the wiki for the different commands that can be passed to your wombat
1. Remember, Clojure will return the last form, so the last form in `bot.clj` will be what your wombat receives.

##### Training your Wombat.

Sometimes it can be difficult communicating with your wombat, seeing as they are completely dependent on your instructions and all. However you are in luck my friend! Closing the feedback loop is crucial to improving your wombat and there is tool that will help you do just that.

1. Clone this repository
1. Install [Boot](http://boot-clj.com/) (a Clojure built tool) `brew install boot-clj`
1. run `boot sim -h` in the root directory of the *wombats-api* repository to view the help menu.

### Development Requirements

1. [leiningen](http://leiningen.org/)
1. [mongodb](https://docs.mongodb.com/)

#### Development ENV Setup

1. [Register](https://github.com/settings/applications/new) a new GitHub OAuth Application.
1. Add the following environment variables.
   - *WOMBATS_GITHUB_CLIENT_ID* (GitHub Client ID)
   - *WOMBATS_GITHUB_CLIENT_SECRET* (GitHub Client Secret)
   - *WOMBATS_OAUTH_SIGNING_SECRET* (Random secret string)
   - *WOMBATS_WEB_CLIENT_URL* (Root URL that the user will be redirected to once Auth is complete)

1. (REQUIRED) Run `lein run` in root directory (builds project and runs server)
1. (REQUIRED - Unless using remote DB) If running DB locally, Run `mongod` to start MongoDB

#### Production ENV Setup

In addition to the development setup, add the following environment variables so connect to a remote datebase.

1. Add the following to connect to a remote DB.
   - *WOMBATS_MONGOD_USER_NAME*
   - *WOMBATS_MONGOD_USER_PW*
   - *WOMBATS_MONGOD_HOST_LIST*