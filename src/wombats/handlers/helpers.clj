(ns wombats.handlers.helpers)

(defn wombat-error
  "Throws an error that will be caught by the exception interceptor."
  ([{:keys [message details code errors]
     :or {message "A problem has occured"
          details {}
          errors []
          code 1}}]
   (wombat-error code
                 message
                 details
                 errors))
  ([code message details errors]
   (let [error-payload {:type :wombat-error
                        :message message
                        :details details
                        :code code}]
     (throw (ex-info "Wombat Error"
                     (if-not (empty? errors)
                       (assoc error-payload :errors errors)
                       error-payload))))))

;; Error Codes
;; Format: a-bb-ccc
;; a: File type: 0 handlers
;;               1 dao
;; b: File number: Increments by one for each file
;; c: Error number: Increments by one for each error
(def game-handler-errors
  {:arena-not-found
   #({:message (str "Arena not found.")
      :details {:arena-id %}
      :code 000000})
   :wombat-not-found
   #({:message (str "Wombat not found")
      :details {:wombat-id %1
                :wombat-eid %2}
      :code 000001})
   :cannot-use-wombat
   #({:message "You do not have permission to use this wombat"
      :details {:requesting-user-eid %1
                :requested-wombat-eid %2}
      :code 000002})
   :game-not-found
   #({:message "This game does not exist"
      :details {:game-id %}
      :code 000003})
   :invalid-game-start-state
   #({:message "This game is not able to be started in its current state"
      :details {:game-id %1
                :game-state %2}
      :code 000004})})

(def user-handler-errors
  {:no-user
   #({:message "User does not exist"
      :details {:user-id %}
      :code 001000})
   :homeless-wombat
   #({:message (str "Oh no!!! " %1 " is homeless! "
                    "Check you url '") %2 "' and make sure you have code at that location."
      :code 001001})})

(def arena-dao-errors
  {:name-taken
   #({:message (str "Arena " % " already exists.")
                  :code 100000})
   :does-not-exist
   #({:message "Arena does not exist"
      :details {:arena-id %}
      :code 100001})})

(def game-dao-errors
  {:not-found
   #({:message "Game could not be found."
      :details (str "Game '" % "' was not found")
      :code 101000})
   :no-open-enrollment
   #({:message "This game is no longer accepting new players."
      :code 101001})
   :already-joined
   #({:message "You have already joined this game"
      :details (str "User '" %1 "' is already in game '" %2 "'")
      :code 101002})
   :color-in-use
   #({:message (str "Color '" % "' is already in use")
      :code 101003})
   :no-user
   #({:details (str "No user-eid was able to be found")
      :code 101004})
   :no-wombat
   #({:details (str "No wombat-eid was able to be found")
      :code 101005})})

(def user-dao-errors
  {:wombat-name-taken
   #({:message (str "Wombat with name '" % "' is already in use")
      :code 102000})
   :wombat-source-taken
   #({:message "Wombat source code was already registered. If you own the source code, change the file name and try again."
      :code 102001})})
