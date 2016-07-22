(set-env! :source-paths #{"src/clj" "tests"}
          :dependencies '[[http-kit "2.1.19"]
                          [base64-clj "0.1.1"]
                          [cheshire "5.1.1"]
                          [com.novemberain/monger "3.0.2"]])

(require '[battlebots.game.frame.processor :refer [process-frame]]
         '[battlebots.game.initializers :refer [initialize-frame]]
         '[battlebots.game.finalizers :refer [finalize-frame]]
         '[battlebots.arena.utils :as au]
         '[battlebots.game.utils :as gu]
         '[battlebots.services.github :refer [decode-bot]]
         '[battlebots.game.test-game :refer [o b a f p]]
         '[org.httpkit.client :as http]
         '[cheshire.core :refer [parse-string]])

(defn- get-bot-code-simulator
  "Used to run in the bot simulator"
  [username repo token]
  (let [request-headers (if token
                          {"Accept" "application/json"
                           "Authorization" (str "token " token)}
                          {"Accept" "application/json"})

        github-url (str "https://api.github.com/repos/"
                        username "/" repo
                        "/contents/bot.clj")

        {:keys [status body headers] :as res} @(http/get
                                                github-url
                                                {:headers request-headers})

        {:keys [x-ratelimit-remaining x-ratelimit-limit x-ratelimit-reset]} headers

        time-till-refresh (- (Integer. x-ratelimit-reset)
                             (quot (System/currentTimeMillis) 1000))]
    (condp = status
        200 {:code (decode-bot (:content (parse-string body true)))
             :ratelimit-message (str (if-not token "Note: You can pass a github API token to increase your rate limit\n")
                                     "You have "
                                     x-ratelimit-remaining
                                     " remaining API calls"
                                     "\nMax Rate Limit: "
                                     x-ratelimit-limit
                                     "\nTime Till Refresh: "
                                     time-till-refresh " seconds")}
        401 (println "Invalid API Token")
        (println "Failed to retrive " username "/" repo "/bot.clj"))))

(defn arena-1
  [player]
  [[o o b o b o]
   [f f p o f p]
   [a b p (gu/sanitize-player player) o b]
   [f f o o b o]
   [f b f o b o]])

(defn arena-2
  [player]
  [[b b b b b b b b b b]
   [b f o o a o o o a b]
   [b o o p o f o o f b]
   [b f o o o o o o f b]
   [b f o o o o o o o b]
   [b p o o o o o o o b]
   [b o o o (gu/sanitize-player player) f o o o b]
   [b o o o p o o b o b]
   [b f o o o o f o o b]
   [b b b b b b b b b b]])

(deftask sim
  "Runs the Battlebots simulator"
  [u username USERNAME  str  "github username"
   r repo     REPO      str  "bot repo"
   e energy   ENERGY    int  "energy"
   f frames   FRAMES    int  "number of frames to process (max 50)"
   t token    TOKEN     str  "github API token"
   a arena    ARENA     int  "arena number: (default 1)

  Arena 1:
   o o b o b o
   f f p o f p
   a b p B o b
   f f o o b o
   f b f o b o

  Arena 2:
   b b b b b b b b b b
   b f o o a o o o a b
   b o p p o f o o f b
   b f o o o o o o f b
   b f o o o o o o f b
   b p o o o o o o o b
   b o o o B f o o o b
   b o o o p o o b o b
   b f o o o o f o o b
   b b b b b b b b b b"]
  (let [{:keys [code ratelimit-message]} (get-bot-code-simulator username repo token)
        arena-number (min 2 (or arena 1))
        player {:_id "1"
                :type "player"
                :login username
                :energy (or energy 100)
                :bot code
                :saved-state {}
                :frames []}
        initial-game-state {:clean-arena ((ns-resolve *ns* (symbol (str "arena-" arena-number))) player)
                            :players [player]}
        initial-frame-count (min 50 (or frames 1))]
    (println "Running Simulation...")

    (when code
      (loop [{:keys [clean-arena messages players] :as game-state} initial-game-state
             frame-count initial-frame-count
             frame-display "Starting Arena"]

        (println "\nFRAME: " frame-display)
        (au/pprint-arena clean-arena)
        (println (str "\n\nMessages: " (or messages {})
                      "\nEnergy: " (:energy (first players))))

        (if (= frame-count 0)
          (println (str "\nDone!\n"
                        ratelimit-message))
          (recur
           ((comp finalize-frame process-frame initialize-frame) game-state)
           (dec frame-count)
           (- initial-frame-count (dec frame-count))))))))
