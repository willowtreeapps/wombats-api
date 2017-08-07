(ns wombats.game.dev-mode.python
  (:require [wombats.game.dev-mode.interpreter :as interpreter]))

(defn handler
  "This evalutes the py bot code against the state given."
  [code state timeout]
  (let [script (str "import json, sys, time\n\n\n"
                    (slurp "src/wombats/lib/python/wombat_lib.py")
                    "\ndef timeout():\n"
                    "    return " timeout " - int(time.time() * 1000)"
                    "\n\n\n"
                    code
                    "\n\n"
                    "json.dump(wombat(json.load(sys.stdin)['state'], timeout),"
                    " sys.stdout)\n")]
    (interpreter/handler ".py" "python" state script)))

