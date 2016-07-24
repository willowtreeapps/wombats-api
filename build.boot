(set-env! :source-paths #{"src/clj" "tests"}
          :dependencies '[[http-kit "2.1.19"]
                          [base64-clj "0.1.1"]
                          [cheshire "5.1.1"]
                          [com.novemberain/monger "3.0.2"]])

(require '[wombats.config.game :as game]
         '[wombats.game.frame.processor :refer [process-frame]]
         '[wombats.game.initializers :refer [initialize-frame]]
         '[wombats.game.finalizers :refer [finalize-frame]]
         '[wombats.arena.utils :as au]
         '[wombats.game.utils :as gu]
         '[wombats.services.github :refer [decode-bot]]
         '[wombats.game.test-game :refer [o b a f p]]
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
  [player player2]
  (let [w (gu/sanitize-player player)
        w2 (gu/sanitize-player player2)
        a1 (assoc a :uuid "a1")
        a2 (assoc a :uuid "a2")
        a3 (assoc a :uuid "a3")]
    [[b b b b b b b b b b b b b b b b]
     [b a1 o o a2 o o o o o o o o o o b]
     [b o o p o f o o f o o p o o o b]
     [b f o o o o o o f o p o o o o b]
     [b f o o o o o o o w2 o o o o o b]
     [b p o o o o o o f o o f o o o b]
     [b b b b o f o o o o o o o o o b]
     [b o o o p o o b o o o o f b o b]
     [b f o o o o f o o f o o o b o b]
     [b f o o o o f o o f o b b b o b]
     [b f o o o o o o o o o o o o o b]
     [b o o o o o f o o o f o f a3 o b]
     [b f o o o o o o o o o o o o o b]
     [b f o o o o f o o o o f o o o b]
     [b w o o o o p o o o o o o o o b]
     [b b b b b b b b b b b b b b b b]]))

(deftask sim
  "Runs the Wombats simulator"
  [u username USERNAME  str  "github username"
   r repo     REPO      str  "bot repo"
   e energy   ENERGY    int  "energy (default 100)"
   f frames   FRAMES    int  "number of frames to process (default 1, max 100)"
   t token    TOKEN     str  "github API token"
   l live               bool "enable live preview (default disabled)"
   s sleep    SLEEP     int  "sleep time in milliseconds when live is enabled (default 2000)"
   a arena    ARENA     int  "arena number: (default 1)

  Arena 1:
   o o b o b o
   f f p o f p
   a b p B o b
   f f o o b o
   f b f o b o

  Arena 2:
    [[b b b b b b b b b b b b b b b b]
     [b a o o a o o o o o o o o o o b]
     [b o o p o f o o f o o p o o o b]
     [b f o o o o o o f o p o o o o b]
     [b f o o o o o o o o o o o o o b]
     [b p o o o o o o f o o f o o o b]
     [b b b b o f o o o o o o o o o b]
     [b o o o p o o b o o o o f b o b]
     [b f o o o o f o o f o o o b o b]
     [b f o o o o f o o f o b b b o b]
     [b f o o o o o o o o o o o o o b]
     [b o o o o o f o o o f o f a o b]
     [b f o o o o o o o o o o o o o b]
     [b f o o o o f o o o o f o o o b]
     [b B o o o o p o o o o o o o o b]
     [b b b b b b b b b b b b b b b b]]"]
  (let [{:keys [code ratelimit-message]} (get-bot-code-simulator username repo token)
        arena-number (min 2 (or arena 1))
        player {:_id "1"
                :type "player"
                :login username
                :energy (or energy 100)
                :bot code
                :saved-state {}
                :frames []}
        player2 (assoc player :_id "2")

        initial-game-state {:clean-arena ((ns-resolve *ns* (symbol (str "arena-" arena-number))) player player2)
                            :players [player player2]}
        initial-frame-count (min 100 (or frames 1))
        sleep-time (or sleep 2000)]
    (println "Running Simulation...")

    (when code
      (loop [{:keys [clean-arena messages players] :as game-state} initial-game-state
             frame-count initial-frame-count
             frame-display "Starting Arena"]

        (when live
          ;; clear screen
          (print (str (char 27) "[2J"))
          ;; move cursor to the top left corner of the screen
          (print (str (char 27) "[;H")))

        (println "\nFRAME: " frame-display)
        (au/pprint-arena clean-arena)
        (println (str "\n\nMessages: " (or messages {})
                      "\nEnergy: " (:energy (first players))))

        (when live
          (Thread/sleep sleep-time))

        (if (= frame-count 0)
          (println (str "\nDone!\n"
                        ratelimit-message))
          (recur
           ((comp finalize-frame #(process-frame % game/config) initialize-frame) game-state)
           (dec frame-count)
           (- initial-frame-count (dec frame-count))))))))
