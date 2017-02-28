(ns wombats.constants)

(defonce github-base "https://github.com/login/oauth/")
(defonce github-authorize-url (str github-base "authorize"))
(defonce github-access-token-url (str github-base "access_token"))
(defonce github-scopes "user:email")
(defonce github-user-profile-url "https://api.github.com/user")
(defonce github-repo-api-base "https://api.github.com/repos/")

(defonce max-players 8)

(defonce min-lambda-runtime 2000)

;; Error Codes
;; Format: a-bb-ccc
;; a: File type: 0 handlers
;;               1 dao
;; b: File number: Increments by one for each file
;; c: Error number: Increments by one for each error
(def errors
  {000000 "Arena not found."
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

(defonce initial-stats
  {:stats/frame-number 0
   :stats/food-collected 0
   :stats/poison-collected 0
   :stats/score 0
   :stats/wombats-destroyed 0
   :stats/wombats-hit 0
   :stats/zakano-destroyed 0
   :stats/zakano-hit 0
   :stats/wood-barriers-destroyed 0
   :stats/wood-barriers-hit 0
   :stats/shots-fired 0
   :stats/shots-hit 0
   :stats/smoke-bombs-thrown 0
   :stats/number-of-moves 0})
