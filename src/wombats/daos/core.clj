(ns wombats.daos.core
  (:require [datomic.api :as d]
            [wombats.daos.user :as user]
            [wombats.daos.arena :as arena]
            [wombats.daos.game :as game]
            [wombats.daos.simulator :as simulator]))

(defn init-dao-map
  "Creates a map of all the data accessors that can be used inside of handlers / socket connections.
  This makes no assumption of authentication / authorization which should be handled prior to gaining
  access to these functions."
  [{:keys [conn] :as datomic}
   aws-credentials]

  {;; User DAOS
   :get-users (user/get-users conn)
   :get-user-by-id (user/get-user-by-id conn)
   :get-entire-user-by-id (user/get-entire-user-by-id conn)
   :get-user-by-email (user/get-user-by-email conn)
   :get-user-by-github-id (user/get-user-by-github-id conn)
   :get-user-by-access-token (user/get-user-by-access-token conn)
   :remove-access-token (user/remove-access-token conn)
   :create-or-update-user (user/create-or-update-user conn)
   ;; Wombat Management DAOS
   :get-user-wombats (user/get-user-wombats conn)
   :get-wombat-owner-id (user/get-wombat-owner-id conn)
   :get-wombat-by-name (user/get-wombat-by-name conn)
   :get-wombat-by-id (user/get-wombat-by-id conn)
   :add-user-wombat (user/add-user-wombat conn)
   :update-user-wombat (user/update-user-wombat conn)
   :retract-wombat (user/retract-wombat conn)
   ;; Arena Management DAOS
   :get-arenas (arena/get-arenas conn)
   :get-arena-by-name (arena/get-arena-by-name conn)
   :get-arena-by-id (arena/get-arena-by-id conn)
   :add-arena (arena/add-arena conn)
   :update-arena (arena/update-arena conn)
   :retract-arena (arena/retract-arena conn)
   ;; Game Management DAOS
   :get-all-games (game/get-all-games conn)
   :get-all-pending-games (game/get-all-pending-games conn)
   :get-game-eids-by-status (game/get-game-eids-by-status conn)
   :get-game-eids-by-player (game/get-game-eids-by-player conn)
   :get-games-by-eids (game/get-games-by-eids conn)
   :get-game-by-id (game/get-game-by-id conn)
   :get-game-state-by-id (game/get-game-state-by-id conn)
   :get-player-from-game (game/get-player-from-game conn)
   :add-game (game/add-game conn)
   :retract-game (game/retract-game conn)
   :add-player-to-game (game/add-player-to-game conn)
   :start-game (game/start-game conn aws-credentials)
   ;; Simulator DAOS
   :get-simulator-arena-templates (simulator/get-simulator-arena-templates conn)
   :get-simulator-arena-template-by-id (simulator/get-simulator-arena-template-by-id conn)})
