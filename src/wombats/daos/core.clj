(ns wombats.daos.core
  (:require [datomic.api :as d]
            [wombats.daos.user :as user]
            [wombats.daos.arena :as arena]
            [wombats.daos.game :as game]))

(defn init-dao-map
  "Creates a map of all the data accessors that can be used inside of handlers / socket connections.
  This makes no assumption of authentication / authorization which should be handled prior to gaining
  access to these functions."
  [{:keys [conn] :as datomic}]
  {;; User DAOS
   :get-users (user/get-users conn)
   :get-user-by-id (user/get-user-by-id conn)
   :get-user-by-email (user/get-user-by-email conn)
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
   :get-games (game/get-games conn)
   :get-game-by-id (game/get-game-by-id conn)
   :add-game (game/add-game conn)
   :retract-game (game/retract-game conn)
   :add-player-to-game (game/add-player-to-game conn)})
