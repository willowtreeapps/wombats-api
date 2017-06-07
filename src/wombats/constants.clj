(ns wombats.constants)

(defonce github-base "https://github.com/login/oauth/")
(defonce github-authorize-url (str github-base "authorize"))
(defonce github-access-token-url (str github-base "access_token"))
(defonce github-scopes "user:email")
(defonce github-user-profile-url "https://api.github.com/user")
(defonce github-repo-api-base "https://api.github.com/repos/")

(defn github-repositories-by-id
  [user-id]
  (str "https://api.github.com/users/" user-id "/repos"))

(defonce max-players 8)

(defonce min-lambda-runtime 2000)

(defonce games-per-page 10)

(def errors
  {:handlers.simulator.initialize-simulator/missing-template "Simulator template '%s' not found."
   :handlers.simulator.initialize-simulator/missing-user "User with id '%s' not found."
   :handlers.simulator.initialize-simulator/missing-wombat "Wombat with id '%s' not found."
   :handlers.access_key.update-access-key-fields/max-number-of-keys "Max number of keys must be greater than or eaual to the number of claimed keys."
   000000 "Arena not found."
   000001 "Wombat cound not be found."
   000002 "You do not have permissions to use this wombat."
   000003 "Game could not be found."
   000004 "Game is not able to be started in its current state."
   000005 "Invalid password."
   001000 "User does not exist."
   001001 "Oh no!!! %s is homeless! Check your url '%s' and make sure code exists at that location."
   100000 "Arena '%s' already exists."
   100001 "Arena does not exist."
   101000 "Game could not be found."
   101001 "This game is no longer accepting new players."
   101002 "You have already joined this game."
   101003 "Color '%s' is already in use."
   101004 "User could not be found."
   101005 "Wombat could not be found."
   101006 "Game cannot be started with no Wombats!"
   101007 "This game has already been started."
   101008 "The game you are trying to join is over."
   101009 "Something went wrong. You are unable to join this game."
   102000 "Wombat with the name '%s' already exists."
   102001 "Wombat source code with that pathname has already registered. If you own the source code, change the file name and try again."})

(def initial-stats
  {:stats/frame-number 0
   :stats/score 0
   :stats/food-collected 0
   :stats/poison-collected 0
   :stats/number-of-moves 0
   :stats/smoke-bombs-thrown 0
   :stats/shots-fired 0
   :stats/shots-hit 0
   :stats/have-been-shot 0
   :stats/wombats-destroyed 0
   :stats/zakano-destroyed 0
   :stats/wombats-shot 0
   :stats/zakano-shot 0
   :stats/wood-barriers-destroyed 0
   :stats/steel-barriers-destroyed 0
   :stats/wood-barriers-shot 0
   :stats/steel-barriers-shot 0
   :stats/deaths 0
   :stats/deaths-by-shot 0
   :stats/deaths-by-wood-barrier-collision 0
   :stats/deaths-by-steel-barrier-collision 0
   :stats/deaths-by-wombat-collision 0
   :stats/deaths-by-zakano-collision 0
   :stats/deaths-by-poison 0
   :stats/wood-barrier-collisions 0
   :stats/steel-barrier-collisions 0
   :stats/zakano-collisions 0
   :stats/wombat-collisions 0
   :stats/frames-blinded 0
   :stats/frames-played 0})

(def game-parameters
  {;; HP Modifiers
   :collision-hp-damage 10
   :food-hp-bonus 5
   :poison-hp-damage 10
   ;; Score Modifiers
   :food-score-bonus 10
   :wombat-hit-bonus 10
   :zakano-hit-bonus 8
   :steel-barrier-hit-bonus 2
   :wood-barrier-hit-bonus 2
   :wombat-destroyed-bonus 25
   :zakano-destroyed-bonus 15
   :wood-barrier-destroyed-bonus 3
   :steel-barrier-destroyed-bonus 25
   ;; In game parameters
   :shot-distance 5})
