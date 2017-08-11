(ns wombats.game.dev-mode.javascript
  (:require [wombats.game.dev-mode.interpreter :as interpreter]))

(defn handler
  "This evaluates the js bot code against the state given."
  [code state timeout]
  (let [script (str "require(\""
                    (System/getProperty "user.dir")
                    "/src/wombats/lib/javascript/wombat_lib.js\")();"
                    "const stdin  = process.stdin,\n"
                    "      stdout = process.stdout;\n\n"
                    "const wombats = "
                    code
                    "\n\nconst timeLeft = " timeout "\n\n"
                    "function timeout() {\n"
                    "  return (timeLeft - Date.now());\n"
                    "}\n\n"
                    "var chunks = [];\n\n"
                    "stdin.resume();\n"
                    "stdin.setEncoding('utf8');\n\n"
                    "stdin.on('data', function(chunk) {\n"
                    "  chunks.push(chunk);\n"
                    "});\n\n"
                    "stdin.on('end', function() {\n"
                    "  var inputJSON  = chunks.join(),\n"
                    "      parsedData = JSON.parse(inputJSON),\n"
                    "      outputData = wombats(parsedData.state, "
                                               "timeout);\n\n"
                    "  stdout.write(JSON.stringify(outputData, "
                                                   "null, '  '));\n"
                    "});\n")]
        (interpreter/handler ".js" "node" state script)))

