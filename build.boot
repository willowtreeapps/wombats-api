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
             :ratelimit-message (str "Note: You can pass a github API token to increase your rate limit\n\n"
                                     "You have "
                                     x-ratelimit-remaining
                                     " ramaining API calls"
                                     "\nMax Limit: "
                                     x-ratelimit-limit
                                     "\nTime till refresh: "
                                     time-till-refresh " seconds")}
        401 (println "Invalid API Token")
        (println "Failed to retrive " username "/" repo "/bot.clj"))))

(deftask sim
  "Runs the Battlebots simulator"
  [u username USERNAME  str  "github username"
   r repo     REPO      str  "bot repo"
   e energy   ENERGY    int  "energy"
   f frames   FRAMES    int  "number of frames to process"
   t token    TOKEN     str  "github API token"]
  (let [{:keys [code ratelimit-message]} (get-bot-code-simulator username repo token)
        player {:_id "1"
                :type "player"
                :login username
                :energy (or energy 100)
                :bot code
                :saved-state {}
                :frames []}
        initial-game-state {:clean-arena [[o o b o b o]
                                          [f f p o f p]
                                          [a b p (gu/sanitize-player player) o b]
                                          [f f o o b o]
                                          [f b f o b o]]
                            :players [player]}]
    (println "Running Simulation...")

    (when code
      (loop [{:keys [clean-arena messages players] :as game-state} initial-game-state
             frame-count (or frames 1)
             frame-display "Starting Arena"]

        (println "\nFrame: " frame-display)
        (au/pprint-arena clean-arena)
        (println (str "\n\nMessages: " (or messages {})
                      "\nEnergy: " (:energy (first players))))

        (if (= frame-count 0)
          (println (str "\nDone!\n"
                        ratelimit-message))
          (recur
           ((comp finalize-frame process-frame initialize-frame) game-state)
           (dec frame-count)
           (- frames (dec frame-count))))))))
